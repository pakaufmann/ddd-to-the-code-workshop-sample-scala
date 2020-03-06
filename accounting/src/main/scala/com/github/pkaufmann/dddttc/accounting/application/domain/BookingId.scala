package com.github.pkaufmann.dddttc.accounting.application.domain

import java.time.{LocalDateTime, ZoneOffset}

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class BookingId private(value: String) extends AnyVal

object BookingId {
  def apply(userId: UserId, startedAt: LocalDateTime) = new BookingId(userId + "-" + startedAt.toEpochSecond(ZoneOffset.UTC))
}
