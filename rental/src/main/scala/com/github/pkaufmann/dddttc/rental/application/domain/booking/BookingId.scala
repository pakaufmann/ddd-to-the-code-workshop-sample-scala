package com.github.pkaufmann.dddttc.rental.application.domain.booking

import cats.Functor
import cats.Functor.ops._
import com.github.pkaufmann.dddttc.domain.UUIDGenerator
import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class BookingId(value: String) extends AnyVal

object BookingId {
  private[domain] def newId[F[_] : Functor](uuidGenerator: UUIDGenerator[F]): F[BookingId] = uuidGenerator.map(id => BookingId(id.toString))
}