package com.github.pkaufmann.dddttc.rental.application

import java.time.Clock

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.{Publisher, Result}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.booking._
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
trait BookingService[F[_]] {
  def bookBike(numberPlate: NumberPlate, userId: UserId): Result[F, BookBikeError, Booking]

  def listBookings(): F[List[Booking]]

  def completeBooking(bookingId: BookingId): Result[F, CompleteBookingError, Unit]
}

object BookingService {

  def apply[F[_] : Monad]
  (
    bookBikeService: BookBikeService[F],
    bookingRepository: BookingRepository[F],
    eventPublisher: Publisher[F, BookingCompletedEvent],
    clock: Clock
  ): BookingService[F] = new Impl[F](bookBikeService, bookingRepository, eventPublisher, clock)

  private class Impl[F[_] : Monad]
  (
    bookBikeService: BookBikeService[F],
    bookingRepository: BookingRepository[F],
    eventPublisher: Publisher[F, BookingCompletedEvent],
    implicit val clock: Clock
  ) extends BookingService[F] {

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
        _ <- (bookingRepository.update(completedBooking) *> eventPublisher(event)).asResult[CompleteBookingError]
      } yield ()
    }
  }

}