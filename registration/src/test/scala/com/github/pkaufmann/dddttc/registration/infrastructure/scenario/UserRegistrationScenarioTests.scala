package com.github.pkaufmann.dddttc.registration.infrastructure.scenario

import java.time.{Clock, Instant, ZoneOffset}

import cats.Id
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event.{PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.TestRegistrations._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.registration.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.registration.infrastructure.persistence.JdbcUserRegistrationRepository
import com.github.pkaufmann.dddttc.testing.AggregateBuilder.{AggregateMatchers, _}
import com.github.pkaufmann.dddttc.testing.{DbTest, RecordingEventPublisher}
import doobie.free.connection.ConnectionIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class UserRegistrationScenarioTests extends AnyFlatSpec with Matchers with AggregateMatchers with DbTest {

  val clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC)
  val eventRecorder = new RecordingEventPublisher[ConnectionIO, RegistrationEvents](
    TransactionalEventPublisher.multi(clock, PendingEventStore)
  )

  val service = UserRegistrationService(JdbcUserRegistrationRepository, eventRecorder)

  "The user registration" should "register a new user" in {
    val userHandle = UserHandle("peter")
    val phoneNumber = PhoneNumber("+41 79 123 45 67")
    val fullName = FullName("Peter", "Meier")

    val (userRegistrationId, verificationCode, result) = run {
      for {
        userRegistrationId <- service.startNewUserRegistrationProcess(userHandle, phoneNumber).leftWiden[Any]
        verificationCode = eventRecorder.getRecorded[PhoneNumberVerificationCodeGeneratedEvent].get.verificationCode
        _ <- service.verifyPhoneNumber(userRegistrationId, verificationCode).leftWiden[Any]
        _ <- service.completeUserRegistration(userRegistrationId, fullName).leftWiden[Any]
        completed <- JdbcUserRegistrationRepository.get(userRegistrationId).leftWiden[Any]
      } yield (userRegistrationId, verificationCode, completed)
    }.getOrElse(fail("Registration was not successful"))

    val expectedCompleted: UserRegistration = TestRegistrations.completed.change
      .replace(Symbol("id"), userRegistrationId)
      .replace(Symbol("verificationCode"), verificationCode)
      .replace(Symbol("fullName"), Option(fullName))
      .back[UserRegistration]

    result should eqv(expectedCompleted)
  }
}
