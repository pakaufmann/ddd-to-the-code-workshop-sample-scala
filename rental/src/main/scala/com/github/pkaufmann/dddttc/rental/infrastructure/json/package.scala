package com.github.pkaufmann.dddttc.rental.infrastructure

import java.util.concurrent.TimeUnit

import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.booking.{BikeUsage, Booking, BookingCompletedEvent, BookingId}
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId}
import io.circe._
import io.circe.generic.semiauto._
import shapeless.LabelledGeneric

import scala.concurrent.duration.FiniteDuration

package object json {
  object implicits {
    implicit val bookingIdEncoder = Encoder.encodeString.contramap[BookingId](_.value)
    implicit val bookingIdDecoder = Decoder.decodeString.map[BookingId](BookingId.apply)

    implicit val numberPlateEncoder = Encoder.encodeString.contramap[NumberPlate](_.value)
    implicit val numberPlateDecoder = Decoder.decodeString.map[NumberPlate](NumberPlate.apply)

    implicit val userIdEncoder = Encoder.encodeString.contramap[UserId](_.value)
    implicit val userIdDecoder = Decoder.decodeString.map[UserId](UserId.apply)

    implicit val finiteDurationEncoder = Encoder.encodeLong.contramap[FiniteDuration](_.toSeconds)
    implicit val finiteDurationDecoder = Decoder.decodeLong.map[FiniteDuration](in => FiniteDuration(in, TimeUnit.SECONDS))

    implicit val bikeUsageCodec = deriveCodec[BikeUsage]

    implicit val labelledBookingCompletedEvent = LabelledGeneric[BookingCompletedEvent]
    implicit val bookingCompletedCodec = deriveCodec[BookingCompletedEvent]

    implicit val labelledUser = LabelledGeneric[User]
    implicit val userCodec = deriveCodec[User]

    implicit val labelledBooking = LabelledGeneric[Booking]
    implicit val bookingCodec = deriveCodec[Booking]

    implicit val labelledBike = LabelledGeneric[Bike]
    implicit val bikeCodec = deriveCodec[Bike]
  }
}
