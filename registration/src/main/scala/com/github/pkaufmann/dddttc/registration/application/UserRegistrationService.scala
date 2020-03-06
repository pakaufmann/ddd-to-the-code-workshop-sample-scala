package com.github.pkaufmann.dddttc.registration.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.{Publisher, Result}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService
import shapeless._

@ApplicationService
trait UserRegistrationService[F[_]] {
  def startNewUserRegistrationProcess(userHandle: UserHandle, phoneNumber: PhoneNumber): Result[F, UserRegistrationError, UserRegistrationId]

  def verifyPhoneNumber(userRegistrationId: UserRegistrationId, verificationCode: VerificationCode): Result[F, VerificationError, Unit]

  def completeUserRegistration(userRegistrationId: UserRegistrationId, fullName: FullName): Result[F, CompleteVerificationError, Unit]
}

object UserRegistrationService {
  type RegistrationEvents = PhoneNumberVerificationCodeGeneratedEvent :+: UserRegistrationCompletedEvent :+: CNil

  def apply[F[_] : Monad](userRegistrationRepository: UserRegistrationRepository[F], publisher: Publisher[F, RegistrationEvents]): UserRegistrationService[F] = {
    new Impl[F](userRegistrationRepository, publisher)
  }

  private class Impl[F[_] : Monad]
  (
    userRegistrationRepository: UserRegistrationRepository[F], publisher: Publisher[F, RegistrationEvents]
  ) extends UserRegistrationService[F] {

    val userRegistrationFactory = UserRegistration(userRegistrationRepository)(_, _)

    def startNewUserRegistrationProcess(userHandle: UserHandle, phoneNumber: PhoneNumber): Result[F, UserRegistrationError, UserRegistrationId] = {
      for {
        userRegistration <- userRegistrationFactory(userHandle, phoneNumber)
        (registration, event) = userRegistration
        _ <- publisher(Coproduct(event)).asResult[UserRegistrationError]
        _ <- userRegistrationRepository.add(registration).leftWiden[UserRegistrationError]
        _ <- if (phoneNumber == PhoneNumber("+41 79 123 45 68")) {
          PhoneNumberNotSwissError(phoneNumber).asLeft[Unit].leftWiden[UserRegistrationError].asResult[F]
        } else {
          ().asRight[UserRegistrationError].asResult[F]
        }
      } yield registration.id
    }

    def verifyPhoneNumber(userRegistrationId: UserRegistrationId, verificationCode: VerificationCode): Result[F, VerificationError, Unit] = {
      for {
        userRegistration <- userRegistrationRepository.get(userRegistrationId)
        verifiedRegistration <- userRegistration.verifyPhoneNumber(verificationCode).asResult[F]
        _ <- userRegistrationRepository.update(verifiedRegistration).leftWiden[VerificationError]
      } yield ()
    }

    def completeUserRegistration(userRegistrationId: UserRegistrationId, fullName: FullName): Result[F, CompleteVerificationError, Unit] = {
      for {
        userRegistration <- userRegistrationRepository.get(userRegistrationId)
        completed <- userRegistration.complete(fullName).asResult[F]
        (completedRegistration, event) = completed
        _ <- publisher(Coproduct[RegistrationEvents](event)).asResult[CompleteVerificationError]
        _ <- userRegistrationRepository.update(completedRegistration).leftWiden[CompleteVerificationError]
      } yield ()
    }
  }

}
