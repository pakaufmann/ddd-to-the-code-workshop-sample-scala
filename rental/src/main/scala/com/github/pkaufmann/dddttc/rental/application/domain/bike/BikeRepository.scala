package com.github.pkaufmann.dddttc.rental.application.domain.bike

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import org.springframework.stereotype.Repository

@Repository
trait BikeRepository[F[_]] {
  def add(bike: Bike): F[Unit]

  def update(bike: Bike): F[Unit]

  def get(numberPlate: NumberPlate): Result[F, BikeNotExistingError, Bike]

  def findAll(): F[List[Bike]]
}
