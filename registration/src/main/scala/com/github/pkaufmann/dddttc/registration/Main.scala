package com.github.pkaufmann.dddttc.registration

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain.SendVerificationCodeEventHandler
import com.github.pkaufmann.dddttc.registration.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.registration.infrastructure.persistence.{JdbcUserRegistrationRepository, Persistence}
import com.github.pkaufmann.dddttc.registration.infrastructure.sms.LoggingSmsNotificationSender
import com.github.pkaufmann.dddttc.registration.infrastructure.web.{UserRegistrationController, defaultErrorHandler}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Persistence.initDb().use { implicit xa =>

      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)
      val eventStore = new PendingEventStore()
      val pendingEventPublisher = new PendingEventPublisher(eventStore, connFactory)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val userRegistrationRepository = new JdbcUserRegistrationRepository()

      val userRegistrationService = new UserRegistrationService(
        userRegistrationRepository,
        TransactionalEventPublisher.multi[RegistrationEvents](clock, eventStore)
      )

      val smsSender = new LoggingSmsNotificationSender[ConnectionIO]()
      SendVerificationCodeEventHandler.register(eventSubscriber.createSubscription(), smsSender)

      for {
        publications <- pendingEventPublisher.start()
        subscriptions <- eventSubscriber.start()
        server <- Server
          .create(
            config.port,
            defaultErrorHandler,
            "/user-registration" -> UserRegistrationController.routes(userRegistrationService)
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
