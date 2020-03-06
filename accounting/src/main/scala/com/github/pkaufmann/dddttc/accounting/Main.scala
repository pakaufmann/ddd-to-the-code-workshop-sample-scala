package com.github.pkaufmann.dddttc.accounting

import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain.BookingFeePolicy
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.{BookingCompletedMessageListener, UserRegistrationCompletedMessageListener}
import com.github.pkaufmann.dddttc.accounting.infrastructure.persistence.{JdbcWalletRepository, Persistence}
import com.github.pkaufmann.dddttc.accounting.infrastructure.web.{AccountingController, defaultErrorHandler}
import com.github.pkaufmann.dddttc.infrastructure.event.{MqEventSubscriber, PendingEventPublisher, PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.infrastructure.web._
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Persistence.initDb().use { implicit transact =>
      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val eventStore = new PendingEventStore()
      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)

      val pendingEventPublisher = new PendingEventPublisher(eventStore, connFactory)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val walletService = new WalletService(
        new BookingFeePolicy(),
        new JdbcWalletRepository(),
        TransactionalEventPublisher(clock, eventStore)
      )

      eventSubscriber.subscribe(BookingCompletedMessageListener(walletService))
      eventSubscriber.subscribe(UserRegistrationCompletedMessageListener(walletService))

      val publisherContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
      val subscriberContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

      for {
        publications <- pendingEventPublisher.start()(publisherContext, implicitly[Timer[IO]])
        subscriptions <- eventSubscriber.start()(subscriberContext)
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
