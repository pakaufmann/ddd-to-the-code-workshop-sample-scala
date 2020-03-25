package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.DomainService

@DomainService
private[application] object BookingFeePolicy {
  private val initialPrice = Amount("1.50")
  private val pricePerMinute = Amount("0.25")

  def feeForBooking(booking: Booking): Amount = {
    val roundedMinutes = (booking.duration.toSeconds / 60.0).round
    this.initialPrice + (this.pricePerMinute * roundedMinutes)
  }
}
