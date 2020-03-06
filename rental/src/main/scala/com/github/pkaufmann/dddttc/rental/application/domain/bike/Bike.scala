package com.github.pkaufmann.dddttc.rental.application.domain.bike

import java.time.Clock

import com.github.pkaufmann.dddttc.rental.application.domain.BikeAlreadyBookedError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.Booking
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId}
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class Bike private(@AggregateId numberPlate: NumberPlate, userId: Option[UserId]) {
  def available: Boolean = userId.isEmpty

  private[domain] def bookBikeFor(user: User)(implicit clock: Clock): Either[BikeAlreadyBookedError, (Bike, Booking)] = {
    if (this.userId.isDefined) {
      return Left(BikeAlreadyBookedError(this))
    }

    Right((
      copy(userId = Some(user.id)),
      Booking(numberPlate, user.id)
    ))
  }

  private[domain] def markAsReturnedBy(userId: UserId): Either[BikeAlreadyBookedError, Bike] = {
    if (!this.userId.contains(userId)) {
      return Left(BikeAlreadyBookedError(this))
    }
    Right(copy(userId = None))
  }
}

private[application] object Bike {
  @AggregateFactory
  def apply(numberPlate: NumberPlate) = new Bike(numberPlate, None)
}