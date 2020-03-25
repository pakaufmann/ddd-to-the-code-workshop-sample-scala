package com.github.pkaufmann.dddttc.rental.application

import cats.Id
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BikeServiceTests extends AnyFlatSpec with Matchers {
  "The bike service" should "return a single bike" in {
    val getBike = BikeService.getBike[Id](
      { case p@NumberPlate("1") => Right(Bike(p)).asResult[BikeNotExistingError, Id] }
    )

    val response = getBike(NumberPlate("1")).value

    response should equal(Right(Bike(NumberPlate("1"))))
  }

  it should "return an error when no bike was found" in {
    val getBike = BikeService.getBike[Id](
      { case p@NumberPlate("1") => Left(BikeNotExistingError(p)).asResult[Bike, Id] }
    )

    getBike(NumberPlate("1")).value should equal(Left(BikeNotExistingError(NumberPlate("1"))))
  }

  it should "list all bikes" in {
    val listBikes = BikeService.listBikes[Id](
      List(Bike(NumberPlate("1")), Bike(NumberPlate("2")))
    )

    listBikes should equal(List(Bike(NumberPlate("1")), Bike(NumberPlate("2"))))
  }
}
