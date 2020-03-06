package com.github.pkaufmann.dddttc.accounting.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.events.Publisher
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
class WalletService[F[_] : Monad](bookingFeePolicy: BookingFeePolicy, walletRepository: WalletRepository[F], domainEventPublisher: Publisher[F, WalletInitializedEvent]) {

  def initializeWallet(userId: UserId): Result[F, WalletAlreadyExistsError, Unit] = {
    val (wallet, event) = Wallet(userId)
    domainEventPublisher.publish(event).asResult[WalletAlreadyExistsError] *> walletRepository.add(wallet)
  }

  def billBookingFee(booking: Booking): Result[F, BillBookingFeeError, Unit] = {
    for {
      wallet <- walletRepository.get(booking.userId)
      billedWallet <- wallet.billBookingFee(booking, this.bookingFeePolicy.feeForBooking).asResult[F]
      result <- walletRepository.update(billedWallet).leftWiden[BillBookingFeeError]
    } yield result
  }

  def listWallets(): F[List[Wallet]] = walletRepository.findAll()
}
