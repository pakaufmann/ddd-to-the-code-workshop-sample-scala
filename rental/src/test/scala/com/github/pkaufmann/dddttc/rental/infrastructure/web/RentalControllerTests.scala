package com.github.pkaufmann.dddttc.rental.infrastructure.web

import cats.effect.SyncIO
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.BikeNotExistingError
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import com.github.pkaufmann.dddttc.testing.AggregateBuilder.create
import com.github.pkaufmann.dddttc.testing.Body
import org.http4s.implicits._
import org.http4s.{Request, Status}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.HNil

class RentalControllerTests extends AnyFlatSpec with Matchers {
  "The rental controller" should "return a list of all existing bikes" in {
    val bike1 = create[Bike].apply(NumberPlate("1") :: Option.empty[UserId] :: HNil)
    val bike2 = create[Bike].apply(NumberPlate("2") :: Option(UserId("1")) :: HNil)

    val listBikes = RentalController.listBikes[SyncIO](
      SyncIO.pure(List(bike1, bike2))
    )

    val result = listBikes
      .run(Request(uri = uri"/bikes"))
      .getOrElse(fail())
      .unsafeRunSync()

    val htmlBody = Body.readHtml(result)

    result.status shouldBe Status.Ok
    htmlBody.select(".form-group") should have size 2
    htmlBody.select(".book-bike-1 .btn-primary").first().attr("hidden") shouldBe empty
    htmlBody.select(".book-bike-2 .btn-primary").first().attr("hidden") shouldBe "hidden"
  }

  it should "return a 200 status when getting an existing bike" in {
    val bike = create[Bike].apply(NumberPlate("1") :: Option.empty[UserId] :: HNil)

    val getBike = RentalController.getBike[SyncIO](
      { case NumberPlate("1") => Right(bike).asResult[BikeNotExistingError, SyncIO] }
    )

    val result = getBike
      .run(Request(uri = uri"/bookings/new?numberPlate=1"))
      .getOrElse(fail())
      .unsafeRunSync()

    result.status shouldBe Status.Ok
    Body.readHtml(result).select(".btn.btn-primary").first().attr("hidden") shouldBe empty
  }

  it should "return not found if the bike does not exist" in {
    val getBike = RentalController.getBike[SyncIO](
      { case p@NumberPlate("1") => Left(BikeNotExistingError(p)).asResult[Bike, SyncIO] }
    )

    val result = getBike
      .run(Request(uri = uri"/bookings/new?numberPlate=1"))
      .getOrElse(fail())
      .unsafeRunSync()

    result.status shouldBe Status.NotFound
    Body.readHtml(result).select(".alert.alert-danger") should not be empty
  }

  it should "deactivate the book button when the bike is not available" in {
    val bike = create[Bike].apply(NumberPlate("1") :: Option(UserId("1")) :: HNil)

    val getBike = RentalController.getBike[SyncIO](
      { case NumberPlate("1") => Right(bike).asResult[BikeNotExistingError, SyncIO] }
    )

    val result = getBike
      .run(Request(uri = uri"/bookings/new?numberPlate=1"))
      .getOrElse(fail())
      .unsafeRunSync()

    Body.readHtml(result).select(".btn.btn-primary").first().attr("hidden") shouldBe "hidden"
  }
}
