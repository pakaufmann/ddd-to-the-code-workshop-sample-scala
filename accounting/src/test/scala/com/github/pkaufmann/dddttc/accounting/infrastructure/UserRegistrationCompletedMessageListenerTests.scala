package com.github.pkaufmann.dddttc.accounting.infrastructure

import cats.Id
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain.{UserId, WalletAlreadyExistsError}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.testing.IdTransaction
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserRegistrationCompletedMessageListenerTests extends AnyFlatSpec with Matchers with MockFactory {

  class MockWalletService extends WalletService[Id](null, null, null)

  implicit val transactor = new IdTransaction[Id]
  private val walletService = mock[MockWalletService]

  "The listener" should "initialize a wallet on the registration completed event" in {
    (walletService.initializeWallet _).expects(UserId("1")) returning Right(()).asResult[WalletAlreadyExistsError, Id]

    val listener = UserRegistrationCompletedMessageListener(walletService)

    listener(UserRegistrationCompletedMessage("1"))
  }
}
