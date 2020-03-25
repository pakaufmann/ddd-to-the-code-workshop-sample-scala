package com.github.pkaufmann.dddttc.rental.application.domain.bike

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import org.springframework.stereotype.Repository

@Repository
object BikeRepository {
  type Add[F[_]] = Bike => F[Unit]

  type Update[F[_]] = Bike => F[Unit]

  type Get[F[_]] = NumberPlate => Result[F, BikeNotExistingError, Bike]

  type FindAll[F[_]] = F[List[Bike]]
}
