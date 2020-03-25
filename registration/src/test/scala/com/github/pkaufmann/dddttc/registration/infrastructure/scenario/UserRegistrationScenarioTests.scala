package com.github.pkaufmann.dddttc.registration.infrastructure.scenario

import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Publisher
import com.github.pkaufmann.dddttc.infrastructure.event.{PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.TestRegistrations._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.registration.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.registration.infrastructure.generator.Generator
import com.github.pkaufmann.dddttc.registration.infrastructure.persistence.JdbcUserRegistrationRepository
import com.github.pkaufmann.dddttc.testing.AggregateBuilder.{AggregateMatchers, _}
import com.github.pkaufmann.dddttc.testing.DbTest
import doobie.free.connection.ConnectionIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class UserRegistrationScenarioTests extends AnyFlatSpec with Matchers with AggregateMatchers with DbTest {

  "The user registration" should "register a new user" in {
    val userHandle = UserHandle("peter")
    val phoneNumber = PhoneNumber("+41 79 123 45 67")
    val fullName = FullName("Peter", "Meier")

    var sentVerificationCode = Option.empty[VerificationCode]

    val recordingPhoneNumberPublisher: Publisher[ConnectionIO, PhoneNumberVerificationCodeGeneratedEvent] =
      event => {
        sentVerificationCode = Some(event.verificationCode)

        TransactionalEventPublisher.single[ConnectionIO, PhoneNumberVerificationCodeGeneratedEvent](
          PendingEventStore.store
        ).apply(event)
      }

    val start = UserRegistrationService.startNewUserRegistrationProcess(
      JdbcUserRegistrationRepository.find, JdbcUserRegistrationRepository.add,
      recordingPhoneNumberPublisher,
      Generator.randomInt[ConnectionIO],
      Generator.uuid[ConnectionIO]
    )

    val verify = UserRegistrationService.verifyPhoneNumber(
      JdbcUserRegistrationRepository.get, JdbcUserRegistrationRepository.update
    )
    val complete = UserRegistrationService.completeUserRegistration(
      JdbcUserRegistrationRepository.get, JdbcUserRegistrationRepository.update,
      TransactionalEventPublisher.single(PendingEventStore.store)
    )

    val (userRegistrationId, result) = run {
      for {
        userRegistrationId <- start(userHandle, phoneNumber).leftWiden[Any]
        _ <- verify(userRegistrationId, sentVerificationCode.get).leftWiden[Any]
        _ <- complete(userRegistrationId, fullName).leftWiden[Any]
        completed <- JdbcUserRegistrationRepository.get(userRegistrationId).leftWiden[Any]
      } yield (userRegistrationId, completed)
    }.getOrElse(fail("Registration was not successful"))

    val expectedCompleted: UserRegistration = TestRegistrations.completed.change
      .replace(Symbol("id"), userRegistrationId)
      .replace(Symbol("verificationCode"), sentVerificationCode.get)
      .replace(Symbol("fullName"), Option(fullName))
      .back[UserRegistration]

    result should eqv(expectedCompleted)
  }
}
