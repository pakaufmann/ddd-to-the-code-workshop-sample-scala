package com.github.pkaufmann.dddttc.rental.application.domain.booking

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BookingNotExistingError
import org.springframework.stereotype.Repository

@Repository
object BookingRepository {
  type FindAll[F[_]] = F[List[Booking]]

  type Add[F[_]] = Booking => F[Unit]

  type Update[F[_]] = Booking => F[Unit]

  type Get[F[_]] = BookingId => Result[F, BookingNotExistingError, Booking]
}
