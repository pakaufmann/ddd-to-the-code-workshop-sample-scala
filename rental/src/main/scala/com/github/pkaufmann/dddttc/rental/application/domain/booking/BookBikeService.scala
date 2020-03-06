package com.github.pkaufmann.dddttc.rental.application.domain.booking

import java.time.Clock

import cats.Monad
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BookBikeError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.user.{UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.DomainService

@DomainService
class BookBikeService[F[_] : Monad](bikeRepository: BikeRepository[F], bookingRepository: BookingRepository[F], userRepository: UserRepository[F], implicit val clock: Clock) {
  private[application] def bookBike(numberPlate: NumberPlate, userId: UserId): Result[F, BookBikeError, Booking] = {
    for {
      bike <- bikeRepository.get(numberPlate)
      user <- userRepository.get(userId)
      bookingResult <- bike.bookBikeFor(user).asResult[F]
      (bookedBike, booking) = bookingResult
      _ <- bikeRepository.update(bookedBike).asResult[BookBikeError]
      _ <- bookingRepository.add(booking).asResult[BookBikeError]
    } yield booking
  }
}