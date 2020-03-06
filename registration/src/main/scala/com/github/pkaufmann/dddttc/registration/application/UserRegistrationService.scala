package com.github.pkaufmann.dddttc.registration.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.events.Publisher
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService.RegistrationEvents
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService
import shapeless._

@ApplicationService
class UserRegistrationService[F[_] : Monad](userRegistrationRepository: UserRegistrationRepository[F], publisher: Publisher[F, RegistrationEvents]) {

  val userRegistrationFactory = UserRegistration(userRegistrationRepository)(_, _)

  def startNewUserRegistrationProcess(userHandle: UserHandle, phoneNumber: PhoneNumber): Result[F, UserRegistrationError, UserRegistrationId] = {
    for {
      userRegistration <- userRegistrationFactory(userHandle, phoneNumber)
      (registration, event) = userRegistration
      _ <- publisher.publish(Coproduct(event)).asResult[UserRegistrationError]
      _ <- userRegistrationRepository.add(registration).leftWiden[UserRegistrationError]
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
      _ <- publisher.publish(Coproduct[RegistrationEvents](event)).asResult[CompleteVerificationError]
      _ <- userRegistrationRepository.update(completedRegistration).leftWiden[CompleteVerificationError]
    } yield ()
  }
}

object UserRegistrationService {
  type RegistrationEvents = PhoneNumberVerificationCodeGeneratedEvent :+: UserRegistrationCompletedEvent :+: CNil
}
