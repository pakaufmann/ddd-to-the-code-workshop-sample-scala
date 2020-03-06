package com.github.pkaufmann.dddttc.registration

import cats.data.{EitherT, NonEmptyList, ReaderT}
import cats.effect._
import cats.implicits._
import cats.tagless.Derive
import cats.tagless.implicits._
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain.{PhoneNumber, SendVerificationCodeEventHandler, UserHandle, UserRegistration, UserRegistrationId, UserRegistrationRepository, VerificationCode}
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
    implicit val userRegistrationServiceFunctorK = Derive.functorK[UserRegistrationService]
    implicit val functorK = Derive.functorK[UserRegistrationRepository]
    implicit val eventPublisherFunctorK = Derive.functorK[EventPublisher]

    Persistence.initDb[IO]().use { implicit xa =>
      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)
      val pendingEventPublisher = EventPublisher.fromStore(PendingEventStore)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val userRegistrationService = UserRegistrationService[ReaderT[ConnectionIO, Trace, *]](
        JdbcUserRegistrationRepository.mapK(conIOToReader[Trace]),
        TransactionalEventPublisher.multiTracing[ConnectionIO, RegistrationEvents](clock, PendingEventStore)
      )

      val smsSender = new LoggingSmsNotificationSender[IO]()

      val publishLoop = MqSession.withSession[IO](connFactory)(pendingEventPublisher.publish().mapK(xa.trans))

      for {
        publications <- publishLoop.start
        subscriptions <- List(
          eventSubscriber.bindTrace(
            SendVerificationCodeEventHandler.onPhoneNumberVerified(smsSender)
          )
        ).traverse(_.start)
        server <- Server
          .create(
            config.port,
            defaultErrorHandler,
            "/user-registration" -> UserRegistrationController.routes(
              userRegistrationService
            )
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
