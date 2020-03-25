package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.domain.{Result, UUIDGenerator}
import com.github.pkaufmann.dddttc.registration.application.domain.VerificationCode.RandomNumber
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class UserRegistration private(@AggregateId id: UserRegistrationId, userHandle: UserHandle, phoneNumber: PhoneNumber, verificationCode: VerificationCode, fullName: Option[FullName], phoneNumberVerified: Boolean, completed: Boolean) {

  def verifyPhoneNumber(code: VerificationCode): Either[PhoneNumberVerificationError, UserRegistration] = {
    if (phoneNumberVerified) {
      return Left(PhoneNumberAlreadyVerifiedError(phoneNumber))
    }
    if (!verificationCode.matches(code)) {
      return Left(PhoneNumberVerificationCodeInvalidError(code))
    }

    Right(copy(phoneNumberVerified = true))
  }

  def complete(fullName: FullName): Either[CompleteError, (UserRegistration, UserRegistrationCompletedEvent)] = {
    if (!phoneNumberVerified) {
      return Left(PhoneNumberNotYetVerifiedError(phoneNumber))
    }

    if (completed) {
      return Left(UserRegistrationAlreadyCompletedError(id))
    }

    Right((
      copy(fullName = Some(fullName), completed = true),
      UserRegistrationCompletedEvent(id, userHandle, phoneNumber, fullName)
    ))
  }
}

private[application] object UserRegistration {
  type UserRegistrationFactory[F[_]] = (UserHandle, PhoneNumber) => Result[F, UserRegistrationError, (UserRegistration, PhoneNumberVerificationCodeGeneratedEvent)]

  @AggregateFactory
  def apply[F[_] : Monad]
  (
    findUser: UserRegistrationRepository.Find[F],
    numberGenerator: RandomNumber[F],
    uuidGenerator: UUIDGenerator[F]
  ): UserRegistrationFactory[F] = {
    val registrationFactory = newRegistration(numberGenerator, uuidGenerator)

    (userHandle, phoneNumber) => {
      for {
        existing <- findUser(userHandle).asResult
        newUser <- existing.fold(
          registrationFactory(userHandle, phoneNumber).leftWiden[UserRegistrationError]
        )(
          _ => UserHandleAlreadyInUseError(userHandle).asErrorResult
        )
      } yield newUser
    }
  }

  private type NewRegistrationFactory[F[_]] = (UserHandle, PhoneNumber) => Result[F, PhoneNumberNotSwissError, (UserRegistration, PhoneNumberVerificationCodeGeneratedEvent)]

  private def newRegistration[F[_] : Monad](random: RandomNumber[F], uuidGenerator: UUIDGenerator[F]): NewRegistrationFactory[F] = {
    (userHandle, phoneNumber) => {
      if (!phoneNumber.isSwiss) {
        PhoneNumberNotSwissError(phoneNumber).asErrorResult
      } else {

        (UserRegistrationId.newId(uuidGenerator), VerificationCode.random(random))
          .mapN { (id, code) =>
            (
              UserRegistration(id, userHandle, phoneNumber, code, None, phoneNumberVerified = false, completed = false),
              PhoneNumberVerificationCodeGeneratedEvent(phoneNumber, code)
            )
          }
          .asResult
      }
    }
  }
}
