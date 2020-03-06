package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class Wallet private(@AggregateId id: UserId, transactions: List[Transaction], balance: Amount) {

  def billBookingFee(booking: Booking, bookingFeePolicy: Booking => Amount): Either[BookingAlreadyBilled, Wallet] = {
    applyTransaction(
      Transaction(
        TransactionReference(booking.id.value),
        bookingFeePolicy(booking).negate()),
      BookingAlreadyBilled(this, booking))
  }

  private def applyTransaction[E](transaction: Transaction, error: => E): Either[E, Wallet] = {
    if (transactions.exists(_.reference == transaction.reference)) {
      Left(error)
    } else {
      val newBalances = transactions :+ transaction
      Right(copy(transactions = newBalances, balance = newBalances.map(_.amount).reduce(_ + _)))
    }
  }
}

private[application] object Wallet {
  @AggregateFactory
  def apply(userId: UserId): (Wallet, WalletInitializedEvent) = {
    val newWallet = Wallet(userId, List.empty, Amount.zero)
    (
      newWallet,
      WalletInitializedEvent(newWallet.id)
    )
  }
}
