package com.github.pkaufmann.dddttc.rental.application

import cats.Monad
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
trait BikeService[F[_]] {
  def addBike(plate: NumberPlate): F[Unit]

  def getBike(numberPlate: NumberPlate): Result[F, BikeNotExistingError, Bike]

  def listBikes(): F[List[Bike]]
}

object BikeService {

  def apply[F[_] : Monad](bikeRepository: BikeRepository[F]): BikeService[F] = new Impl[F](bikeRepository)

  private class Impl[F[_] : Monad](bikeRepository: BikeRepository[F]) extends BikeService[F] {
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

}