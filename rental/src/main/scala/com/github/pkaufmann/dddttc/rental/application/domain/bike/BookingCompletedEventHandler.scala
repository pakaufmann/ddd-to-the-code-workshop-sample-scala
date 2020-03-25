package com.github.pkaufmann.dddttc.rental.application.domain.bike

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.rental.application.domain.ReleaseBikeError
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingCompletedEvent
import com.github.pkaufmann.dddttc.stereotypes.DomainEventHandler
import com.github.pkaufmann.dddttc.domain.implicits._

@DomainEventHandler
object BookingCompletedEventHandler {
  def apply[F[_] : Monad]
  (
    getBike: BikeRepository.Get[F], updateBike: BikeRepository.Update[F]
  ): Subscription[F, BookingCompletedEvent] = {
    event => {
      val result = for {
        bike <- getBike(event.numberPlate)
        result <- bike.markAsReturnedBy(event.userId)
          .traverse(updateBike)
          .asResult[ReleaseBikeError]
      } yield result
      result.value.map(_ => ())
    }
  }
}
