package com.github.pkaufmann.dddttc.rental.application

import cats.Id
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BikeServiceTests extends AnyFlatSpec with MockFactory with Matchers {
  val bikeRepository = mock[BikeRepository[Id]]

  val service = new BikeService(bikeRepository)

  "The bike service" should "return a single bike" in {
    val bike = Bike(NumberPlate("1"))

    (bikeRepository.get _).expects(NumberPlate("1")) returning Right(bike).asResult[BikeNotExistingError, Id]
    val response = service.getBike(NumberPlate("1")).value
    response should equal(Right(bike))
  }

  it should "return an error when no bike was found" in {
    (bikeRepository.get _).expects(NumberPlate("1")) returning Left(BikeNotExistingError(NumberPlate("1"))).asResult[Bike, Id]
    val response = service.getBike(NumberPlate("1")).value
    response should equal(Left(BikeNotExistingError(NumberPlate("1"))))
  }

  it should "list all bikes" in {
    (bikeRepository.findAll _).expects() returning List(Bike(NumberPlate("1")), Bike(NumberPlate("2")))

    val response = service.listBikes()

    response should equal(List(Bike(NumberPlate("1")), Bike(NumberPlate("2"))))
  }
}
