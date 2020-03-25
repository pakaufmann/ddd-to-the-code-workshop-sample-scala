package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BookingNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.{Booking, BookingRepository}
import doobie._
import doobie.implicits._

object JdbcBookingRepository {
  val add: BookingRepository.Add[ConnectionIO] = {
    booking => {
      sql"INSERT INTO booking(id, data) VALUES (${booking.id}, $booking)"
        .update
        .run
        .map(_ => ())
    }
  }

  val update: BookingRepository.Update[ConnectionIO] = {
    booking => {
      sql"UPDATE booking SET data=$booking WHERE id=${booking.id}"
        .update
        .run
        .map(_ => ())
    }
  }

  val get: BookingRepository.Get[ConnectionIO] = {
    bookingId => {
      sql"SELECT data FROM booking WHERE id = $bookingId"
        .query[Booking]
        .option
        .map(_.toRight(BookingNotExistingError(bookingId)))
        .asResult
    }
  }

  val findAll: BookingRepository.FindAll[ConnectionIO] = {
    sql"SELECT data FROM booking".query[Booking]
      .stream
      .compile
      .toList
  }
}
