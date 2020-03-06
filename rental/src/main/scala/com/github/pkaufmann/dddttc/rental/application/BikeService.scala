package com.github.pkaufmann.dddttc.rental.application

import cats.Monad
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
class BikeService[F[_] : Monad](bikeRepository: BikeRepository[F]) {
  def addBike(plate: NumberPlate): F[Unit] = {
    bikeRepository.add(Bike(plate))
  }

  def getBike(numberPlate: NumberPlate): Result[F, BikeNotExistingError, Bike] = {
    bikeRepository.get(numberPlate)
  }

  def listBikes(): F[List[Bike]] = {
    bikeRepository.findAll()
  }
}
