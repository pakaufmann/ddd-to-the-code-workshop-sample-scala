package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Id
import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.TestRegistrations._
import com.github.pkaufmann.dddttc.testing.AggregateBuilder.{AggregateMatchers, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class UserRegistrationTests extends AnyFlatSpec with Matchers with AggregateMatchers with MockFactory {
  val userRegistrationRepository = mock[UserRegistrationRepository[Id]]

  val userRegistrationFactory = UserRegistration(userRegistrationRepository)(_, _)

  "The user registration" should "create a new registration for a valid user handle and phone number" in {
    val handle = UserHandle("peter")
    val number = PhoneNumber("+41 79 123 45 67")

    (userRegistrationRepository.find _).expects(handle) returning None

    val (registration, event) = userRegistrationFactory(handle, number).value.getOrElse(fail())

    val expectedRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("id"), registration.id)
      .replace(Symbol("verificationCode"), registration.verificationCode)
      .back[UserRegistration]

    registration should eqv(expectedRegistration)(fullEq)
    event shouldBe PhoneNumberVerificationCodeGeneratedEvent(number, registration.verificationCode)
  }

  it should "return an error when the user handle is already in use" in {
    val handle = UserHandle("peter")
    val number = PhoneNumber("+41 79 123 45 67")

    (userRegistrationRepository.find _).expects(handle) returning Some(TestRegistrations.default)

    val result = userRegistrationFactory(handle, number)

    result.value shouldBe Left(UserHandleAlreadyInUseError(handle))
  }

  it should "verify the phone number and mark it as verified" in {
    val result = TestRegistrations.default.verifyPhoneNumber(VerificationCode("123456"))

    result.getOrElse(fail()).phoneNumberVerified shouldBe true
  }

  it should "return an error when the verification code is invalid" in {
    val code = VerificationCode("invalid")
    val result = TestRegistrations.default.verifyPhoneNumber(code)

    result shouldBe Left(PhoneNumberVerificationCodeInvalidError(code))
  }

  it should "return an error when the phone number was already verified" in {
    val code = VerificationCode("invalid")
    val verifiedRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("phoneNumberVerified"), true)
      .back[UserRegistration]

    val result = verifiedRegistration.verifyPhoneNumber(code)

    result shouldBe Left(PhoneNumberAlreadyVerifiedError(verifiedRegistration.phoneNumber))
  }

  it should "mark the registration as complete and return an event when a full name is provided and the phone number was verified" in {
    val verifiedRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("phoneNumberVerified"), true)
      .back[UserRegistration]
    val fullName = FullName("Peter", "Smith")

    val (registration, event) = verifiedRegistration.complete(fullName).getOrElse(fail())

    registration.completed shouldBe true
    registration.fullName shouldBe Some(fullName)
    event shouldBe UserRegistrationCompletedEvent(verifiedRegistration.id, verifiedRegistration.userHandle, verifiedRegistration.phoneNumber, fullName)
  }

  it should "return an error if a non-verified registration is tried to be completed" in {
    val unverifiedRegistration = TestRegistrations.default
    val fullName = FullName("Peter", "Smith")

    val result = unverifiedRegistration.complete(fullName)

    result shouldBe Left(PhoneNumberNotYetVerifiedError(unverifiedRegistration.phoneNumber))
  }

  it should "return an error if an already completed registration is tried to be completed again" in {
    val fullName = FullName("Peter", "Smith")

    val completedRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("phoneNumberVerified"), true)
      .replace(Symbol("completed"), true)
      .replace(Symbol("fullName"), Option(fullName))
      .back[UserRegistration]

    val result = completedRegistration.complete(fullName)

    result shouldBe Left(UserRegistrationAlreadyCompletedError(completedRegistration.id))
  }
}
