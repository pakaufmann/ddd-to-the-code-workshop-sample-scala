package com.github.pkaufmann.dddttc.accounting.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.domain.{Publisher, Result}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
trait WalletService[F[_]] {
  def initializeWallet(userId: UserId): Result[F, WalletAlreadyExistsError, Unit]

  def billBookingFee(booking: Booking): Result[F, BillBookingFeeError, Unit]

  def listWallets(): F[List[Wallet]]
}

object WalletService {

  def apply[F[_] : Monad]
  (
    bookingFeePolicy: BookingFeePolicy, walletRepository: WalletRepository[F],
    publisher: Publisher[F, WalletInitializedEvent]
  ): WalletService[F] =
    new Impl[F](bookingFeePolicy, walletRepository, publisher)

  private class Impl[F[_] : Monad](bookingFeePolicy: BookingFeePolicy, walletRepository: WalletRepository[F], publisher: Publisher[F, WalletInitializedEvent]) extends WalletService[F] {

    def initializeWallet(userId: UserId): Result[F, WalletAlreadyExistsError, Unit] = {
    val (wallet, event) = Wallet (userId)
    publisher (event).asResult[WalletAlreadyExistsError] *> walletRepository.add (wallet)
  }

    def billBookingFee(booking: Booking): Result[F, BillBookingFeeError, Unit] = {
    for {
    wallet <- walletRepository.get (booking.userId)
    billedWallet <- wallet.billBookingFee (booking, this.bookingFeePolicy.feeForBooking).asResult[F]
    result <- walletRepository.update (billedWallet).leftWiden[BillBookingFeeError]
  } yield result
  }

    def listWallets(): F[List[Wallet]] = walletRepository.findAll ()
  }

}