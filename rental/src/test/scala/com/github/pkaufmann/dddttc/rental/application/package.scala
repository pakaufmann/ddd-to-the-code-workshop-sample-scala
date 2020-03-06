package com.github.pkaufmann.dddttc.rental

import cats.Eq
import com.github.pkaufmann.dddttc.rental.application.domain.booking.Booking
import shapeless.LabelledGeneric
import shapeless.record._

import scala.language.implicitConversions

package object application {
  implicit val eqBooking = Eq.instance[Booking]((x, y) => LabelledGeneric[Booking].to(x).remove(Symbol("id"))._2 == LabelledGeneric[Booking].to(y).remove(Symbol("id"))._2)
}
