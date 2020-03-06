package com.github.pkaufmann.dddttc.accounting

import java.util.concurrent.Executors

import cats.data.{NonEmptyList, ReaderT}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats.tagless.Derive
import cats.tagless.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain.{BookingFeePolicy, WalletRepository}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.BookingCompletedMessageListener.Message.BookingCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.{BookingCompletedMessageListener, UserRegistrationCompletedMessageListener}
import com.github.pkaufmann.dddttc.accounting.infrastructure.persistence.{JdbcWalletRepository, Persistence}
import com.github.pkaufmann.dddttc.accounting.infrastructure.web.{AccountingController, defaultErrorHandler}
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import doobie.free.connection.ConnectionIO
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    implicit val walletServiceFunctorK = Derive.functorK[WalletService]
    implicit val eventPublisherFunctorK = Derive.functorK[EventPublisher]
    implicit val walletRepositoryFunctorK = Derive.functorK[WalletRepository]

    Persistence.initDb[IO]().use { implicit xa =>
      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)

      val pendingEventPublisher = EventPublisher.fromStore(PendingEventStore)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val walletService = WalletService[ReaderT[ConnectionIO, Trace, *]](
        new BookingFeePolicy(),
        JdbcWalletRepository.mapK(conIOToReader),
        TransactionalEventPublisher.singleTracing(clock, PendingEventStore)
      )

      val publisherContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
      val subscriberContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

      val publishLoop = MqSession.withSession(connFactory)(pendingEventPublisher.publish().mapK(xa.trans))

      for {
        publications <- publishLoop.start(publisherContext)
        subscriptions <- List(
          eventSubscriber.bindTrace[IO, BookingCompletedMessage](BookingCompletedMessageListener(walletService).andThen(_.mapK(xa.trans))),
          eventSubscriber.bindTrace[IO, UserRegistrationCompletedMessage](UserRegistrationCompletedMessageListener(walletService).andThen(_.mapK(xa.trans)))
        ).traverse(_.start(subscriberContext))
        server <- Server
          .create(
            config.port,
            defaultErrorHandler,
            "/accounting" -> AccountingController.routes(walletService)
          )
          .start
        exitCode <- NonEmptyList(
          server,
          subscriptions :+ publications.map(_ => ExitCode.Error)
        ).joinAll
      } yield exitCode
    }
  }
}
