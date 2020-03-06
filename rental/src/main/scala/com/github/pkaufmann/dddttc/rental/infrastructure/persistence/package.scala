package com.github.pkaufmann.dddttc.rental.infrastructure

import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.booking.{Booking, BookingId}
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId}
import com.github.pkaufmann.dddttc.rental.infrastructure.json.implicits._
import doobie._
import io.circe.parser.decode
import io.circe.syntax._

package object persistence {
  implicit val userIdMeta: Meta[UserId] = Meta[String].timap(UserId.apply)(_.value)
  implicit val bookingIdMeta: Meta[BookingId] = Meta[String].timap(BookingId.apply)(_.value)
  implicit val numberPlateMeta: Meta[NumberPlate] = Meta[String].timap(NumberPlate.apply)(_.value)

  implicit val userMeta: Meta[User] = Meta[String].timap(u => decode[User](u).left.map(e => throw e).merge)(_.asJson.noSpaces)
  implicit val bookingMeta: Meta[Booking] = Meta[String].timap(u => decode[Booking](u).left.map(e => throw e).merge)(_.asJson.noSpaces)
}
