package com.github.pkaufmann.dddttc.rental.application.domain.booking

import java.time.{Clock, LocalDateTime}

import com.github.pkaufmann.dddttc.rental.application.domain.BookingAlreadyCompletedError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class Booking private(@AggregateId id: BookingId, numberPlate: NumberPlate, userId: UserId, startedAt: LocalDateTime, endedAt: Option[LocalDateTime], completed: Boolean) {
  private[application] def completeBooking()(implicit clock: Clock): Either[BookingAlreadyCompletedError, (Booking, BookingCompletedEvent)] = {
    if (this.completed) {
      return Left(BookingAlreadyCompletedError(id))
    }

    val endedAt = LocalDateTime.now(clock)

    Right((
      copy(completed = true, endedAt = Some(endedAt)),
      BookingCompletedEvent(id, numberPlate, userId, BikeUsage(startedAt, endedAt))
    ))
  }
}

private[application] object Booking {
  @AggregateFactory
  def apply(numberPlate: NumberPlate, userId: UserId)(implicit clock: Clock): Booking = {
    Booking(BookingId.newId(), numberPlate, userId, LocalDateTime.now(clock), None, completed = false)
  }
}
