package com.github.pkaufmann.dddttc.rental.application.domain.booking

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BookingNotExistingError
import org.springframework.stereotype.Repository

@Repository
trait BookingRepository[F[_]] {
  def findAll(): F[List[Booking]]

  def add(booking: Booking): F[Unit]

  def update(booking: Booking): F[Unit]

  def get(bookingId: BookingId): Result[F, BookingNotExistingError, Booking]
}
