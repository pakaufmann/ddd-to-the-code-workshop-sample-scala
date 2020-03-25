package com.github.pkaufmann.dddttc.rental.application.domain.bike

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeAlreadyBookedError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.Booking
import com.github.pkaufmann.dddttc.rental.application.domain.booking.Booking.BookingFactory
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId}
import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class Bike private(@AggregateId numberPlate: NumberPlate, userId: Option[UserId]) {
  def available: Boolean = userId.isEmpty

  private[domain] def bookBikeFor[F[_] : Monad](bookingFactory: BookingFactory[F])(user: User): Result[F, BikeAlreadyBookedError, (Bike, Booking)] = {
    if (this.userId.isDefined) {
      return BikeAlreadyBookedError(this).asErrorResult
    }

    bookingFactory(numberPlate, user.id)
      .map { booking =>
        (copy(userId = Some(user.id)), booking).asRight[BikeAlreadyBookedError]
      }
      .asResult
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