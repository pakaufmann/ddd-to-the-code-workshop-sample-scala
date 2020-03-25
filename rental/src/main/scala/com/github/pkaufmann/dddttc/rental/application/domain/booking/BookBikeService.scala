package com.github.pkaufmann.dddttc.rental.application.domain.booking

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.domain.{Instant, Result, UUIDGenerator}
import com.github.pkaufmann.dddttc.rental.application.domain.BookBikeError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.user.{UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.DomainService

@DomainService
private[application] object BookBikeService {
  type BookBike[F[_]] = (NumberPlate, UserId) => Result[F, BookBikeError, Booking]

  def bookBike[F[_] : Monad]
  (
    getBike: BikeRepository.Get[F],
    getUser: UserRepository.Get[F],
    updateBike: BikeRepository.Update[F],
    addBooking: BookingRepository.Add[F],
    instant: Instant[F],
    uuidGenerator: UUIDGenerator[F]
  ): BookBike[F] = {
    val bookingFactory = Booking(instant, uuidGenerator)

    (numberPlate, userId) => {
      for {
        bike <- getBike(numberPlate)
        user <- getUser(userId)
        bookingResult <- bike.bookBikeFor(bookingFactory)(user)
        (bookedBike, booking) = bookingResult
        _ <- updateBike(bookedBike).asResult[BookBikeError]
        _ <- addBooking(booking).asResult[BookBikeError]
      } yield booking
    }
  }
}