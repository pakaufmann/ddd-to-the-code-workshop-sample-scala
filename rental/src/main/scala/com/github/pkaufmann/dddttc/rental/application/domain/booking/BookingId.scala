package com.github.pkaufmann.dddttc.rental.application.domain.booking

import java.util.UUID

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class BookingId(value: String) extends AnyVal

object BookingId {
  private[domain] def newId() = BookingId(UUID.randomUUID().toString)
}