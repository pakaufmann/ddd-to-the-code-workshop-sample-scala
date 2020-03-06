package com.github.pkaufmann.dddttc.accounting.application.domain

sealed trait BillBookingFeeError

case class WalletAlreadyExistsError(userId: UserId)

case class WalletNotExistingError(userId: UserId) extends BillBookingFeeError

case class BookingAlreadyBilled(wallet: Wallet, booking: Booking) extends BillBookingFeeError
