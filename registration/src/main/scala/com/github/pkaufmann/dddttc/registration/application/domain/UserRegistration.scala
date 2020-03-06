package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class UserRegistration(@AggregateId id: UserRegistrationId, userHandle: UserHandle, phoneNumber: PhoneNumber, verificationCode: VerificationCode, fullName: Option[FullName], phoneNumberVerified: Boolean, completed: Boolean) {

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
  @AggregateFactory
  def apply[F[_] : Monad](userRegistrationRepository: UserRegistrationRepository[F])(userHandle: UserHandle, phoneNumber: PhoneNumber)(): Result[F, UserRegistrationError, (UserRegistration, PhoneNumberVerificationCodeGeneratedEvent)] = {
    for {
      existing <- userRegistrationRepository.find(userHandle).asResult[UserRegistrationError]
      newUser = existing match {
        case Some(_) => Left(UserHandleAlreadyInUseError(userHandle))
        case None => newRegistration(userHandle, phoneNumber)
      }
      result <- newUser.leftWiden[UserRegistrationError].asResult[F]
    } yield result
  }

  private def newRegistration(userHandle: UserHandle, phoneNumber: PhoneNumber): Either[PhoneNumberNotSwissError, (UserRegistration, PhoneNumberVerificationCodeGeneratedEvent)] = {
    if (!phoneNumber.isSwiss) {
      return Left(PhoneNumberNotSwissError(phoneNumber))
    }
    val verificationCode = VerificationCode.random()

    Right((
      UserRegistration(UserRegistrationId.newId(), userHandle, phoneNumber, verificationCode, None, phoneNumberVerified = false, completed = false),
      PhoneNumberVerificationCodeGeneratedEvent(phoneNumber, verificationCode)
    ))
  }
}
