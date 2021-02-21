package com.github.pkaufmann.dddttc.registration

import cats.data.{EitherT, NonEmptyList, ReaderT}
import cats.effect.{Clock, ExitCode, IO, IOApp}
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.domain.{PhoneNumber, PhoneNumberVerificationCodeGeneratedEvent, SendVerificationCodeEventHandler, UserHandle, UserRegistrationCompletedEvent, UserRegistrationError}
import com.github.pkaufmann.dddttc.registration.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.registration.infrastructure.generator.Generator
import com.github.pkaufmann.dddttc.registration.infrastructure.persistence.{JdbcUserRegistrationRepository, Persistence}
import com.github.pkaufmann.dddttc.registration.infrastructure.sms.LoggingSmsNotificationSender
import com.github.pkaufmann.dddttc.registration.infrastructure.web.{UserRegistrationController, defaultErrorHandler}
import doobie.free.connection.ConnectionIO
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import doobie.implicits._

import javax.jms.ConnectionFactory
import javax.naming.{Context, InitialContext}
import scala.concurrent.duration._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val config = if (args.contains("local")) {
      ConfigSource.resources("application-local.conf").loadOrThrow[ApplicationConfig]
    } else {
      ConfigSource.resources("application.conf").loadOrThrow[ApplicationConfig]
    }

    Persistence.initDb[IO](config.driver, config.url, config.user, config.password).use { implicit xa =>
      implicit val conIOClock = Clock.create[ConnectionIO]

      val connFactory = if(config.brokerType == "local") {
        new ActiveMQConnectionFactory(config.brokerUrl)
      } else {
        AzureConnectionFactory(config.brokerUrl, config.brokerPassword)
      }

      val publish = MqEventPublisher.publish(
        PendingEventStore.getUnsent, PendingEventStore.removeSent
      )

      val publishLoop = MqSession.withSession[IO](connFactory)(publish.transact(xa))

      val startNewUserRegistrationProcess = UserRegistrationService.startNewUserRegistrationProcess[ReaderT[ConnectionIO, Trace, *]](
        JdbcUserRegistrationRepository.find.andThen(_.liftReader),
        JdbcUserRegistrationRepository.add.andThen(_.liftReader),
        TransactionalEventPublisher.singleTracing(PendingEventStore.store),
        Generator.randomInt[ConnectionIO].andThen(_.liftReader),
        Generator.uuid[ConnectionIO].liftReader
      )

      val verifyPhoneNumber = UserRegistrationService.verifyPhoneNumber[ConnectionIO](
        JdbcUserRegistrationRepository.get,
        JdbcUserRegistrationRepository.update
      )

      val completeUserRegistration = UserRegistrationService.completeUserRegistration[ReaderT[ConnectionIO, Trace, *]](
        JdbcUserRegistrationRepository.get(_).liftReader,
        JdbcUserRegistrationRepository.update2(_).liftReader,
        TransactionalEventPublisher.singleTracing[ConnectionIO, UserRegistrationCompletedEvent](PendingEventStore.store)
      )

      val sendVerificationHandler = SendVerificationCodeEventHandler
        .onPhoneNumberVerified[ReaderT[IO, Trace, *]](
          LoggingSmsNotificationSender.sendSmsTo[IO]
            .chaosMonkey(0.2)
            .logErrors(org.log4s.getLogger("SendSMSLogger"))
            .retry(3, 500.millis)
        )
        .measure(time => IO(println(s"Send verification had: $time ns")).liftReader)

      for {
        publications <- publishLoop.start
        subscriptions <- List(
          MqEventSubscriber.bindTrace[IO, PhoneNumberVerificationCodeGeneratedEvent](connFactory, sendVerificationHandler)
        ).traverse(_.start)
        server <- Server
          .create[IO](
            config.port,
            defaultErrorHandler[IO],
            "/user-registration" -> (
              UserRegistrationController.root[IO] <+>
                UserRegistrationController.start(startNewUserRegistrationProcess.andThen(_.transact(xa))) <+>
                UserRegistrationController.verify(verifyPhoneNumber.andThen(_.transact(xa))) <+>
                UserRegistrationController.complete(completeUserRegistration.andThen(_.transact(xa)))
              )
          )
          .use(_ => IO.never)
          .as(ExitCode.Success)
          .start
        exitCode <- NonEmptyList(
          server,
          subscriptions :+ publications.map(_ => ExitCode.Error)
        ).joinAll
      } yield exitCode
    }
  }
}
