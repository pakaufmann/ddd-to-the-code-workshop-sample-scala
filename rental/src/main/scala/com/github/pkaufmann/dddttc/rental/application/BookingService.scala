package com.github.pkaufmann.dddttc.rental.application

import java.time.Clock

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.events.Publisher
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.booking._
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
class BookingService[F[_] : Monad](bookBikeService: BookBikeService[F], bookingRepository: BookingRepository[F], eventPublisher: Publisher[F, BookingCompletedEvent], implicit val clock: Clock) {

  def bookBike(numberPlate: NumberPlate, userId: UserId): Result[F, BookBikeError, Booking] = {
    bookBikeService.bookBike(numberPlate, userId)
  }

  def listBookings(): F[List[Booking]] = {
    bookingRepository.findAll()
  }

  def completeBooking(bookingId: BookingId): Result[F, CompleteBookingError, Unit] = {
    for {
      booking <- bookingRepository.get(bookingId)
      completed <- booking.completeBooking().asResult[F]
      (completedBooking, event) = completed
      _ <- (bookingRepository.update(completedBooking) *> eventPublisher.publish(event)).asResult[CompleteBookingError]
    } yield ()
  }
}
