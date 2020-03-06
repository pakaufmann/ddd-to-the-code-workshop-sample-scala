package com.github.pkaufmann.dddttc.rental.application.domain.bike

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.ReleaseBikeError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingCompletedEvent
import com.github.pkaufmann.dddttc.stereotypes.DomainEventHandler

@DomainEventHandler
object BookingCompletedEventHandler {
  def onBookingCompleted[F[_] : Monad](bikeRepository: BikeRepository[F]): Subscription[F, BookingCompletedEvent] = {
    event => {
      val result = for {
        bike <- bikeRepository.get(event.numberPlate)
        result <- bike.markAsReturnedBy(event.userId)
          .traverse(bikeRepository.update)
          .asResult
          .leftWiden[ReleaseBikeError]
      } yield result

      result.fold(_ => (), _ => ())
    }
  }
}
