package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BookingNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.{Booking, BookingId, BookingRepository}
import doobie._
import doobie.implicits._

class JdbcBookingRepository extends BookingRepository[ConnectionIO] {
  override def add(booking: Booking): ConnectionIO[Unit] = {
    sql"INSERT INTO booking(id, data) VALUES (${booking.id}, $booking)"
      .update
      .run
      .map(_ => ())
  }

  override def update(booking: Booking): ConnectionIO[Unit] = {
    sql"UPDATE booking SET data=$booking WHERE id=${booking.id}"
      .update
      .run
      .map(_ => ())
  }

  override def get(bookingId: BookingId): Result[ConnectionIO, BookingNotExistingError, Booking] = {
    sql"SELECT data FROM booking WHERE id = $bookingId"
      .query[Booking]
      .option
      .map(_.toRight(BookingNotExistingError(bookingId)))
      .asResult
  }

  override def findAll(): ConnectionIO[List[Booking]] = {
    sql"SELECT data FROM booking".query[Booking]
      .stream
      .compile
      .toList
  }
}
