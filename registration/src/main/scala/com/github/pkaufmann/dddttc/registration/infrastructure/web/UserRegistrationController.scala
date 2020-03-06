package com.github.pkaufmann.dddttc.registration.infrastructure.web

import cats.Monad
import cats.effect.IO
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.registration.infrastructure.web.Decoders._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.twirl._

object UserRegistrationController {
  def routes[F[_]: IOTransaction : Monad](userRegistrationService: UserRegistrationService[F]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root =>
        Ok(registration.html.start(None, None))
      case req@POST -> Root / "start" =>
        req.decode[NewUserRegistrationRequest] { data =>
          userRegistrationService
            .startNewUserRegistrationProcess(data.userHandle, data.phoneNumber)
            .transact
            .foldF(
              {
                case PhoneNumberNotSwissError(phoneNumber) =>
                  Ok(registration.html.start(Some(s"Phone number '${phoneNumber.value}' was not swiss"), Some(data)))
                case UserHandleAlreadyInUseError(userHandle) =>
                  Ok(registration.html.start(Some(s"User handle '${userHandle.value}' is already used"), Some(data)))
              },
              userRegistrationId => Ok(registration.html.verify(data.userHandle, userRegistrationId))
            )
        }
      case req@POST -> Root / "verify" =>
        req.decode[VerifyPhoneNumberRequest] { data =>
          userRegistrationService
            .verifyPhoneNumber(data.userRegistrationId, data.verificationCode)
            .transact
            .foldF(
              error => {
                val errorMessage = error match {
                  case PhoneNumberAlreadyVerifiedError(phoneNumber) =>
                    s"Phone number ${phoneNumber.value} was already verified"
                  case PhoneNumberVerificationCodeInvalidError(verificationCode) =>
                    s"Verification code '${verificationCode.value}' was invalid"
                  case UserRegistrationNotExistingError(userRegistrationId) =>
                    s"User for registration id '${userRegistrationId.value}' does not exist"
                }
                Ok(registration.html.verify(data.userHandle, data.userRegistrationId, Some(errorMessage), Some(data)))
              },
              _ => Ok(registration.html.complete(data.userHandle, data.userRegistrationId))
            )
        }
      case req@POST -> Root / "complete" =>
        req.decode[CompleteRegistrationRequest] { data =>
          userRegistrationService
            .completeUserRegistration(data.userRegistrationId, FullName(data.firstName, data.lastName))
            .transact
            .foldF(
              error => {
                val errorMessage = error match {
                  case PhoneNumberNotYetVerifiedError(phoneNumber) =>
                    s"Phone number '${phoneNumber.value}' is not yet verified"
                  case UserRegistrationAlreadyCompletedError(userRegistrationId) =>
                    s"User registration with id '${userRegistrationId.value}' was already completed"
                  case UserRegistrationNotExistingError(userRegistrationId) =>
                    s"User registration with id '${userRegistrationId.value}' does not exist"
                }

                Ok(registration.html.complete(data.userHandle, data.userRegistrationId, Some(errorMessage), Some(data)))
              },
              _ => Ok(registration.html.done(data.userHandle, FullName(data.firstName, data.lastName)))
            )
        }
    }
  }

}
