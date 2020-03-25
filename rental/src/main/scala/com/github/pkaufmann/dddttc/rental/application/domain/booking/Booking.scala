package com.github.pkaufmann.dddttc.rental.application.domain.booking

import java.time.{LocalDateTime, ZoneOffset}

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.{Instant, UUIDGenerator}
import com.github.pkaufmann.dddttc.rental.application.domain.BookingAlreadyCompletedError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class Booking private(@AggregateId id: BookingId, numberPlate: NumberPlate, userId: UserId, startedAt: LocalDateTime, endedAt: Option[LocalDateTime], completed: Boolean) {
  private[application] def completeBooking(ts: java.time.Instant): Either[BookingAlreadyCompletedError, (Booking, BookingCompletedEvent)] = {
    if (this.completed) {
      return Left(BookingAlreadyCompletedError(id))
    }

    val endedAt = LocalDateTime.ofInstant(ts, ZoneOffset.UTC)
    Right((
      copy(completed = true, endedAt = Some(endedAt)),
      BookingCompletedEvent(id, numberPlate, userId, BikeUsage(startedAt, endedAt))
    ))
  }
}

private[application] object Booking {
  type BookingFactory[F[_]] = (NumberPlate, UserId) => F[Booking]

  @AggregateFactory
  def apply[F[_] : Monad](instant: Instant[F], uuidGenerator: UUIDGenerator[F]): BookingFactory[F] = {
    (numberPlate, userId) => {
      for {
        startedAt <- instant.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
        bookingId <- BookingId.newId(uuidGenerator)
      } yield Booking(bookingId, numberPlate, userId, startedAt, None, completed = false)
    }
  }
}
