package com.github.pkaufmann.dddttc.accounting.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.domain.{Publisher, Result}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
object WalletService {
  type InitializeWallet[F[_]] = UserId => Result[F, WalletAlreadyExistsError, Unit]

  type BillBookingFee[F[_]] = Booking => Result[F, BillBookingFeeError, Unit]

  type ListWallets[F[_]] = F[List[Wallet]]

  def initializeWallet[F[_] : Monad]
  (
    addWallet: WalletRepository.Add[F],
    publisher: Publisher[F, WalletInitializedEvent]
  ): InitializeWallet[F] = {
    userId => {
      val (wallet, event) = Wallet(userId).bimap(addWallet, publisher)
      event.asResult[WalletAlreadyExistsError] *> wallet
    }
  }

  def billBookingFee[F[_] : Monad]
  (
    getWallet: WalletRepository.Get[F],
    updateWallet: WalletRepository.Update[F]
  ): BillBookingFee[F] = {
    booking => {
      for {
        wallet <- getWallet(booking.userId)
        billedWallet <- wallet.billBookingFee(booking, BookingFeePolicy.feeForBooking).asResult[F]
        result <- updateWallet(billedWallet).leftWiden[BillBookingFeeError]
      } yield result
    }
  }

  def listWallets[F[_]](findAll: WalletRepository.FindAll[F]): ListWallets[F] = findAll
}