package com.github.pkaufmann.dddttc.registration.application

import cats.implicits._
import cats.{Applicative, Monad}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.domain.{Publisher, Result, UUIDGenerator}
import com.github.pkaufmann.dddttc.registration.application.domain.VerificationCode.RandomNumber
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService
import org.apache.camel.spi.UuidGenerator

@ApplicationService
object UserRegistrationService {
  type StartNewUserRegistrationProcess[F[_]] = (UserHandle, PhoneNumber) => Result[F, UserRegistrationError, UserRegistrationId]

  type VerifyPhoneNumber[F[_]] = (UserRegistrationId, VerificationCode) => Result[F, VerificationError, Unit]

  type CompleteUserRegistration[F[_]] = (UserRegistrationId, FullName) => Result[F, CompleteVerificationError, Unit]

  def startNewUserRegistrationProcess[F[_] : Monad]
  (
    findUser: UserRegistrationRepository.Find[F], addUser: UserRegistrationRepository.Add[F],
    publisher: Publisher[F, PhoneNumberVerificationCodeGeneratedEvent],
    randomIntGenerator: RandomNumber[F],
    uuidGenerator: UUIDGenerator[F]
  ): StartNewUserRegistrationProcess[F] = {
    val userRegistrationFactory = UserRegistration(findUser, randomIntGenerator, uuidGenerator)

    (userHandle, phoneNumber) => {
      for {
        userRegistration <- userRegistrationFactory(userHandle, phoneNumber)
        (registration, event) = userRegistration
        _ <- publisher(event).asResult[UserRegistrationError]
        _ <- addUser(registration).leftWiden[UserRegistrationError]
      } yield registration.id
    }
  }

  def verifyPhoneNumber[F[_] : Monad]
  (
    getUser: UserRegistrationRepository.Get[F], updateUser: UserRegistrationRepository.Update[F]
  ): VerifyPhoneNumber[F] = {
    (userRegistrationId, verificationCode) => {
      for {
        userRegistration <- getUser(userRegistrationId)
        verifiedRegistration <- userRegistration.verifyPhoneNumber(verificationCode).asResult[F]
        _ <- updateUser(verifiedRegistration).leftWiden[VerificationError]
      } yield ()
    }
  }

  def completeUserRegistration[F[_] : Monad]
  (
    getUser: UserRegistrationRepository.Get[F], updateUser: UserRegistrationRepository.Update[F],
    publisher: Publisher[F, UserRegistrationCompletedEvent]
  ): CompleteUserRegistration[F] = {
    (userRegistrationId, fullName) => {
      for {
        userRegistration <- getUser(userRegistrationId)
        completed <- userRegistration.complete(fullName).asResult[F]
        (update, publish) = completed.bimap(updateUser, publisher)
        _ <- update.leftWiden[CompleteVerificationError] *> publish.asResult
      } yield ()
    }
  }
}
