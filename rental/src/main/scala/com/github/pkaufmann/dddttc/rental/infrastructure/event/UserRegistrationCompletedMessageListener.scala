package com.github.pkaufmann.dddttc.rental.infrastructure.event

import cats.Functor
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.rental.application.UserService
import com.github.pkaufmann.dddttc.rental.application.domain.UserAlreadyExistsError
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import io.circe.generic.semiauto._
import org.log4s._

object UserRegistrationCompletedMessageListener {
  private val logger = getLogger

  def apply[F[_] : Functor](userService: UserService[F]): Subscription[F, UserRegistrationCompletedMessage] =
    message => {
      userService
        .addUser(UserId(message.userHandle))
        .fold(
          {
            case UserAlreadyExistsError(userId) =>
              logger.info(s"User already exists. Probably a duplicated message for user: $userId")
          },
          _ => ()
        )
    }

  object Message {

    case class UserRegistrationCompletedMessage(userHandle: String)

    implicit val userRegistrationCompletedMessageDecoder = deriveDecoder[UserRegistrationCompletedMessage]
  }

}
