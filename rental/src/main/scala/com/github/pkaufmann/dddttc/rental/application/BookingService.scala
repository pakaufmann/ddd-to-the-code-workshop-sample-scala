package com.github.pkaufmann.dddttc.rental.application

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.domain.{Instant, Publisher, Result, UUIDGenerator}
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.booking._
import com.github.pkaufmann.dddttc.rental.application.domain.user.{UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
object BookingService {
  type BookBike[F[_]] = (NumberPlate, UserId) => Result[F, BookBikeError, Booking]

  type ListBookings[F[_]] = F[List[Booking]]

  type CompleteBooking[F[_]] = BookingId => Result[F, CompleteBookingError, Unit]

  def bookBike[F[_] : Monad]
  (
    getBike: BikeRepository.Get[F], updateBike: BikeRepository.Update[F],
    getUser: UserRepository.Get[F],
    addBooking: BookingRepository.Add[F],
    instant: Instant[F],
    uuidGenerator: UUIDGenerator[F]
  ): BookBike[F] = BookBikeService.bookBike(getBike, getUser, updateBike, addBooking, instant, uuidGenerator)

  def listBookings[F[_]](findAll: BookingRepository.FindAll[F]): ListBookings[F] = findAll

  def completeBooking[F[_] : Monad]
  (
    getBooking: BookingRepository.Get[F], updateBooking: BookingRepository.Update[F],
    publisher: Publisher[F, BookingCompletedEvent],
    instant: Instant[F]
  ): CompleteBooking[F] = {
    bookingId => {
      for {
        booking <- getBooking(bookingId)
        completed <- instant.map(booking.completeBooking).asResult
        (completedBooking, event) = completed
        _ <- (updateBooking(completedBooking) *> publisher(event)).asResult[CompleteBookingError]
      } yield ()
    }
  }
}
