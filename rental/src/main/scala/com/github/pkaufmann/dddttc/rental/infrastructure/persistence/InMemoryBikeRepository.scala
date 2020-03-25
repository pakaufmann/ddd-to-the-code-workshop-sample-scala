package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import cats.effect.Sync
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.infrastructure.json.implicits._
import io.circe.parser.decode
import io.circe.syntax._

object InMemoryBikeRepository {
  private var bikes = Map.empty[NumberPlate, String]

  def add[F[_] : Sync]: BikeRepository.Add[F] = {
    bike =>
      Sync[F].delay {
        bikes += (bike.numberPlate -> bike.asJson.noSpaces)
      }
  }

  def update[F[_] : Sync]: BikeRepository.Update[F] = {
    bike =>
      Sync[F].delay {
        bikes += (bike.numberPlate -> bike.asJson.noSpaces)
      }
  }

  def get[F[_] : Sync]: BikeRepository.Get[F] = {
    numberPlate =>
      Sync[F].delay {
        bikes.get(numberPlate)
          .map(b => decode[Bike](b).left.map(e => throw e).merge)
          .toRight(BikeNotExistingError(numberPlate))
      }.asResult
  }

  def findAll[F[_] : Sync]: BikeRepository.FindAll[F] = {
    Sync[F].delay {
      bikes.values
        .map(b => decode[Bike](b).left.map(e => throw e).merge)
        .toList
    }
  }
}
