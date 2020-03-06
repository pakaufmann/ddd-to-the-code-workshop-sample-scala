package com.github.pkaufmann.dddttc.accounting.application.domain

import java.time.LocalDateTime

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

import scala.concurrent.duration.FiniteDuration

@ValueObject
case class Booking private(id: BookingId, userId: UserId, duration: FiniteDuration)

object Booking {
  def apply(userId: UserId, startedAt: LocalDateTime, duration: FiniteDuration): Booking =
    new Booking(BookingId(userId, startedAt), userId, duration)
}
