package com.github.pkaufmann.dddttc.registration.application

import java.util.UUID

import cats.Id
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import com.github.pkaufmann.dddttc.testing._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class UserRegistrationServiceTests extends AnyFlatSpec with Matchers {

  "The user registration service" should "start a new registration process" in {
    val handle = UserHandle("peter")

    val start = UserRegistrationService.startNewUserRegistrationProcess[Id](
      { case UserHandle("peter") => None },
      always(Right(()).asResult[UserHandleAlreadyInUseError, Id]),
      always(()),
      always(111111),
      UUID.fromString("02a4c319-f001-461e-8855-13092e973c97")
    )

    start(handle, PhoneNumber("+41 79 123 45 67")).value shouldBe Right(UserRegistrationId("02a4c319-f001-461e-8855-13092e973c97"))
  }

  it should "return an error when the user handle already exists" in {
    val handle = UserHandle("peter")

    val start = UserRegistrationService.startNewUserRegistrationProcess[Id](
      { case UserHandle("peter") => Some(TestRegistrations.default) },
      always(Right(()).asResult[UserHandleAlreadyInUseError, Id]),
      always(()),
      always(111111),
      UUID.randomUUID()
    )

    start(handle, PhoneNumber("+41 79 123 45 67")).value shouldBe Left(UserHandleAlreadyInUseError(handle))
  }

  it should "verify the phone number if an existing user registration and a valid verification code is provided" in {
    val registrationId = UserRegistrationId("user-registration-id-1")
    val verificationCode = VerificationCode("123456")
    val userRegistration = TestRegistrations.default

    val verify = UserRegistrationService.verifyPhoneNumber[Id](
      { case UserRegistrationId("user-registration-id-1") => Right(userRegistration).asResult[UserRegistrationNotExistingError, Id] },
      always(Right(()).asResult[UserRegistrationNotExistingError, Id])
    )

    val result = verify(registrationId, verificationCode)
    result.value.isRight shouldBe true
  }

  it should "complete the user registration if an existing user registration and a full name is provided" in {
    val registrationId = UserRegistrationId("user-registration-id-1")
    val fullName = FullName("Peter", "Smith")
    val userRegistration: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("phoneNumberVerified"), true)
      .back[UserRegistration]

    val complete = UserRegistrationService.completeUserRegistration[Id](
      { case UserRegistrationId("user-registration-id-1") => Right(userRegistration).asResult[UserRegistrationNotExistingError, Id] },
      { case UserRegistration(_, _, _, _, Some(`fullName`), _, true) => Right(()).asResult[UserRegistrationNotExistingError, Id] },
      always(())
    )

    complete(registrationId, fullName).value.isRight shouldBe true
  }
}
