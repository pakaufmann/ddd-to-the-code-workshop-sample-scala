package com.github.pkaufmann.dddttc.rental.application

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
object BikeService {
  type AddBike[F[_]] = NumberPlate => F[Unit]

  type GetBike[F[_]] = NumberPlate => Result[F, BikeNotExistingError, Bike]

  type ListBikes[F[_]] = F[List[Bike]]

  def addBike[F[_]](addBike: BikeRepository.Add[F]): AddBike[F] = addBike.compose(Bike.apply)

  def getBike[F[_]](getBike: BikeRepository.Get[F]): GetBike[F] = getBike

  def listBikes[F[_]](findAll: BikeRepository.FindAll[F]): ListBikes[F] = findAll
}