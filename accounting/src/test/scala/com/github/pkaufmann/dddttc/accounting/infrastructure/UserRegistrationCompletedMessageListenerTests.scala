package com.github.pkaufmann.dddttc.accounting.infrastructure

import cats.Id
import com.github.pkaufmann.dddttc.accounting.application.domain.{UserId, WalletAlreadyExistsError}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.domain.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserRegistrationCompletedMessageListenerTests extends AnyFlatSpec with Matchers {
  "The listener" should "initialize a wallet on the registration completed event" in {
    val listener = UserRegistrationCompletedMessageListener[Id](
      { case UserId("1") => Right(()).asResult[WalletAlreadyExistsError, Id] }
    )

    listener(UserRegistrationCompletedMessage("1"))
  }
}
