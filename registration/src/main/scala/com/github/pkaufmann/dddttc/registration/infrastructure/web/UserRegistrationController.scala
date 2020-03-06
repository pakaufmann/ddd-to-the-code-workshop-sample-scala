package com.github.pkaufmann.dddttc.registration.infrastructure.web

import cats.data.ReaderT
import cats.effect.Sync
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.registration.application.UserRegistrationService
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.registration.infrastructure.web.Decoders._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.http4s.dsl.io._
import org.http4s.twirl._
import org.http4s.{HttpRoutes, Response, Status}

object UserRegistrationController {
  def routes[F[_] : Sync](userRegistrationService: UserRegistrationService[ReaderT[ConnectionIO, Trace, *]])(implicit xa: Transactor[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root =>
        Response[F](status = Status.Ok)
          .withEntity(registration.html.start(None, None))
          .pure[F]
      case req@POST -> Root / "start" =>
        req.decode[NewUserRegistrationRequest] { data =>
          val trace = data.trace.getOrElse(Trace())
          userRegistrationService
            .startNewUserRegistrationProcess(data.userHandle, data.phoneNumber)
            .run(trace)
            .transact(xa)
            .fold(
              {
                case PhoneNumberNotSwissError(phoneNumber) =>
                  Response(status = Status.BadRequest)
                    .withEntity(registration.html.start(Some(s"Phone number '${phoneNumber.value}' was not swiss"), Some(data), Some(trace)))
                case UserHandleAlreadyInUseError(userHandle) =>
                  Response(status = Status.BadRequest)
                    .withEntity(registration.html.start(Some(s"User handle '${userHandle.value}' is already used"), Some(data), Some(trace)))
              },
              userRegistrationId =>
                Response(status = Status.Ok)
                  .withEntity(registration.html.verify(data.userHandle, userRegistrationId, trace))
            )
        }
      case req@POST -> Root / "verify" =>
        req.decode[VerifyPhoneNumberRequest] { data =>
          userRegistrationService
            .verifyPhoneNumber(data.userRegistrationId, data.verificationCode)
            .run(data.trace)
            .transact(xa)
            .fold(
              error => {
                val errorMessage = error match {
                  case PhoneNumberAlreadyVerifiedError(phoneNumber) =>
                    s"Phone number ${phoneNumber.value} was already verified"
                  case PhoneNumberVerificationCodeInvalidError(verificationCode) =>
                    s"Verification code '${verificationCode.value}' was invalid"
                  case UserRegistrationNotExistingError(userRegistrationId) =>
                    s"User for registration id '${userRegistrationId.value}' does not exist"
                }
                Response(status = Status.Ok)
                  .withEntity(registration.html.verify(data.userHandle, data.userRegistrationId, data.trace, Some(errorMessage), Some(data)))
              },
              _ => Response(status = Status.Ok)
                .withEntity(registration.html.complete(data.userHandle, data.userRegistrationId, data.trace))
            )
        }
      case req@POST -> Root / "complete" =>
        req.decode[CompleteRegistrationRequest] { data =>
          userRegistrationService
            .completeUserRegistration(data.userRegistrationId, FullName(data.firstName, data.lastName))
            .run(data.trace)
            .transact(xa)
            .fold(
              error => {
                val errorMessage = error match {
                  case PhoneNumberNotYetVerifiedError(phoneNumber) =>
                    s"Phone number '${phoneNumber.value}' is not yet verified"
                  case UserRegistrationAlreadyCompletedError(userRegistrationId) =>
                    s"User registration with id '${userRegistrationId.value}' was already completed"
                  case UserRegistrationNotExistingError(userRegistrationId) =>
                    s"User registration with id '${userRegistrationId.value}' does not exist"
                }

                Response(status = Status.Ok)
                  .withEntity(registration.html.complete(data.userHandle, data.userRegistrationId, data.trace, Some(errorMessage), Some(data)))
              },
              _ => Response(status = Status.Ok)
                .withEntity(registration.html.done(data.userHandle, FullName(data.firstName, data.lastName), data.trace))
            )
        }
    }
  }

}
