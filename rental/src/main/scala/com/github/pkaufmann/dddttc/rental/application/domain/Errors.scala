package com.github.pkaufmann.dddttc.rental.application.domain

import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingId
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId

sealed trait BookBikeError

sealed trait CompleteBookingError

sealed trait ReleaseBikeError

case class BikeAlreadyBookedError(bike: Bike) extends BookBikeError with ReleaseBikeError

case class BikeNotExistingError(numberPlate: NumberPlate) extends BookBikeError with ReleaseBikeError

case class UserNotExistingError(userId: UserId) extends BookBikeError

case class UserAlreadyExistsError(userId: UserId)

case class BookingAlreadyCompletedError(bookingId: BookingId) extends CompleteBookingError

case class BookingNotExistingError(bookingId: BookingId) extends CompleteBookingError