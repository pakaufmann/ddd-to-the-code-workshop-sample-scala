package com.github.pkaufmann.dddttc.accounting

import java.util.concurrent.Executors
import cats.data.{NonEmptyList, ReaderT}
import cats.effect.{Clock, ContextShift, ExitCode, IO, IOApp}
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.BookingCompletedMessageListener.Message.BookingCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.{BookingCompletedMessageListener, UserRegistrationCompletedMessageListener}
import com.github.pkaufmann.dddttc.accounting.infrastructure.persistence.{JdbcWalletRepository, Persistence}
import com.github.pkaufmann.dddttc.accounting.infrastructure.web.{AccountingController, defaultErrorHandler}
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import doobie.free.connection.ConnectionIO
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import doobie.implicits._

import javax.jms.ConnectionFactory
import javax.naming.{Context, InitialContext}
import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val config = if (args.contains("local")) {
      ConfigSource.resources("application-local.conf").loadOrThrow[ApplicationConfig]
    } else {
      ConfigSource.resources("application.conf").loadOrThrow[ApplicationConfig]
    }

    Persistence.initDb[IO](config.driver, config.url, config.user, config.password).use { implicit xa =>
      implicit val conIOClock = Clock.create[ConnectionIO]

      val connFactory = if (config.brokerType == "local") {
        new ActiveMQConnectionFactory(config.brokerUrl)
      } else {
        AzureConnectionFactory(config.brokerUrl, config.brokerPassword)
      }

      val publish = MqEventPublisher.publish(
        PendingEventStore.getUnsent, PendingEventStore.removeSent
      )

      val publishLoop = MqSession.withSession[IO](connFactory)(publish.transact(xa))

      val billBookingFee = WalletService.billBookingFee[ConnectionIO](
        JdbcWalletRepository.get,
        JdbcWalletRepository.update
      )

      val initializeWallet = WalletService.initializeWallet[ReaderT[ConnectionIO, Trace, *]](
        JdbcWalletRepository.add(_).liftReader,
        TransactionalEventPublisher.singleTracing(PendingEventStore.store)
      )

      val listWallets = WalletService.listWallets[ConnectionIO](
        JdbcWalletRepository.findAll
      )

      val publisherContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))

      val bookingCompletedListener = BookingCompletedMessageListener(billBookingFee).andThen(_.transact(xa))
      val userRegistrationListener = UserRegistrationCompletedMessageListener(initializeWallet).andThen(_.mapF(_.transact(xa)))

      for {
        publications <- publishLoop.start(publisherContext)
        subscriptions <- List(
          MqEventSubscriber.bind[IO, BookingCompletedMessage](connFactory, bookingCompletedListener),
          MqEventSubscriber.bindTrace[IO, UserRegistrationCompletedMessage](connFactory, userRegistrationListener)
        ).traverse(_.start)
        server <- Server
          .create[IO](
            config.port,
            defaultErrorHandler[IO],
            "/accounting" -> AccountingController.listWallets(listWallets.transact(xa))
          )
          .use(_ => IO.never)
          .as(ExitCode.Success)
          .start
        exitCode <- NonEmptyList(
          server,
          subscriptions :+ publications.as(ExitCode.Error)
        ).joinAll
      } yield exitCode
    }
  }
}
