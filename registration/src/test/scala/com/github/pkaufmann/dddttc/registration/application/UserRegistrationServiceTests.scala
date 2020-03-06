package com.github.pkaufmann.dddttc.registration.application

import cats.Id
import com.github.pkaufmann.dddttc.domain.Publisher
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.TestRegistrations._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class UserRegistrationServiceTests extends AnyFlatSpec with Matchers with MockFactory {

  val userRegistrationRepository = mock[UserRegistrationRepository[Id]]
  val publisher = mock[Publisher[Id, RegistrationEvents]]

  val service = UserRegistrationService(userRegistrationRepository, publisher)

  "The user registration service" should "start a new registration process" in {
    val handle = UserHandle("peter")

    (userRegistrationRepository.find _).expects(handle) returning None
    (userRegistrationRepository.add _).expects(*) returning Right(()).asResult[UserHandleAlreadyInUseError, Id]
    (publisher.apply _).expects(*) returning()

    val result = service.startNewUserRegistrationProcess(handle, PhoneNumber("+41 79 123 45 67"))
    result.value.isRight shouldBe true
  }

  it should "verify the phone number if an existing user registration and a valid verification code is provided" in {
    val registrationId = UserRegistrationId("user-registration-id-1")
    val verificationCode = VerificationCode("123456")
    val userRegistration = TestRegistrations.default

    (userRegistrationRepository.get _).expects(registrationId) returning Right(userRegistration).asResult[UserRegistrationNotExistingError, Id]
    (userRegistrationRepository.update _).expects(*) returning Right(()).asResult[UserRegistrationNotExistingError, Id]

    val result = service.verifyPhoneNumber(registrationId, verificationCode)
    result.value.isRight shouldBe true
  }

  it should "complete the user registration if an existing user registration and a full name is provided" in {
    val registrationId = UserRegistrationId("user-registration-id-1")
    val fullName = FullName("Peter", "Smith")
    val userRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("phoneNumberVerified"), true)
      .back[UserRegistration]

    val completedUserRegistration: UserRegistration = userRegistration.change
      .replace(Symbol("completed"), true)
      .replace(Symbol("fullName"), Option(fullName))
      .back[UserRegistration]

    (userRegistrationRepository.get _).expects(registrationId) returning Right(userRegistration).asResult[UserRegistrationNotExistingError, Id]
    (userRegistrationRepository.update _).expects(whereEqv(completedUserRegistration)) returning Right(()).asResult[UserRegistrationNotExistingError, Id]
    (publisher.apply _).expects(*) returning()

    val result = service.completeUserRegistration(registrationId, fullName)
    result.value.isRight shouldBe true
  }
}
