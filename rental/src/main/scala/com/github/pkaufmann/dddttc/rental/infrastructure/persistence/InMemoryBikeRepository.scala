package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import cats.effect.Sync
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.infrastructure.json.implicits._
import io.circe.parser.decode
import io.circe.syntax._

class InMemoryBikeRepository[F[_] : Sync] extends BikeRepository[F] {
  private var bikes = Map.empty[NumberPlate, String]

  override def add(bike: Bike): F[Unit] = {
    Sync[F].delay({
      bikes += (bike.numberPlate -> bike.asJson.noSpaces)
    })
  }

  override def update(bike: Bike): F[Unit] = {
    Sync[F].delay({
      bikes += (bike.numberPlate -> bike.asJson.noSpaces)
    });
  }

  override def get(numberPlate: NumberPlate): Result[F, BikeNotExistingError, Bike] = {
    Sync[F]
      .delay({
        bikes.get(numberPlate)
          .map(b => decode[Bike](b).left.map(e => throw e).merge)
          .toRight(BikeNotExistingError(numberPlate))
      })
      .asResult
  }

  override def findAll(): F[List[Bike]] = {
    Sync[F].delay(bikes.values.map(b => decode[Bike](b).left.map(e => throw e).merge).toList)
  }
}
