package com.github.pkaufmann.dddttc.accounting.infrastructure.event

import cats.Functor
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain.{UserId, WalletAlreadyExistsError}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import io.circe.generic.semiauto.deriveDecoder
import org.log4s._

object UserRegistrationCompletedMessageListener {
  private val logger = getLogger

  def apply[F[_]: Functor](walletService: WalletService[F]): UserRegistrationCompletedMessage => F[Unit] = (message: UserRegistrationCompletedMessage) => {
    walletService
      .initializeWallet(UserId(message.userHandle))
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
