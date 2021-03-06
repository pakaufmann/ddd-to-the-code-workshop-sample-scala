package com.github.pkaufmann.dddttc.accounting.infrastructure.event

import cats.Functor
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain.{UserId, WalletAlreadyExistsError}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.domain.Subscription
import io.circe.generic.semiauto.deriveDecoder
import org.log4s._

object UserRegistrationCompletedMessageListener {
  private val logger = getLogger

  def apply[F[_] : Functor](initializeWallet: WalletService.InitializeWallet[F]): Subscription[F, UserRegistrationCompletedMessage] =
    message => {
      initializeWallet(UserId(message.userHandle))
        .fold(
          {
            case WalletAlreadyExistsError(userId) =>
              logger.info(s"Wallet already existed, probably a duplicated message for user id $userId")
          },
          _ => ()
        )
    }

  object Message {

    case class UserRegistrationCompletedMessage(userHandle: String)

    implicit val userRegistrationDecoder = deriveDecoder[UserRegistrationCompletedMessage]
  }

}
