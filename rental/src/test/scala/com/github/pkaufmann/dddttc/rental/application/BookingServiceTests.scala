package com.github.pkaufmann.dddttc.rental.application

import java.time._
import java.util.UUID

import cats.{Id, _}
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.booking._
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId}
import com.github.pkaufmann.dddttc.rental.application.domain.{BikeAlreadyBookedError, BikeNotExistingError, UserNotExistingError}
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.HNil
import shapeless.record._

class BookingServiceTests extends AnyFlatSpec with Matchers {
  "The booking service" should "create a new booking" in {
    val booking = create[Booking].apply(
      BookingId("02a4c319-f001-461e-8855-13092e973c97") ::
        NumberPlate("1") ::
        UserId("1") ::
        LocalDateTime.ofEpochSecond(1000, 0, ZoneOffset.UTC) ::
        Option.empty ::
        false ::
        HNil
    )

    val bookBike = BookingService.bookBike[Id](
      { case p@NumberPlate("1") => Right(Bike(p)).asResult[BikeNotExistingError, Id] },
      { case Bike(NumberPlate("1"), Some(UserId("1"))) => () },
      { case u@UserId("1") => Right(User(u)).asResult[UserNotExistingError, Id] },
      { case Booking(_, NumberPlate("1"), UserId("1"), t, None, false) if t.toEpochSecond(ZoneOffset.UTC) == 1000 => () },
      Instant.ofEpochSecond(1000),
      UUID.fromString("02a4c319-f001-461e-8855-13092e973c97")
    )

    val result = bookBike(NumberPlate("1"), UserId("1"))

    result.getOrElse(fail()) shouldBe booking
  }

  it should "return an error when the user could not be found" in {
    val bookBike = BookingService.bookBike[Id](
      { case p@NumberPlate("1") => Right(Bike(p)).asResult[BikeNotExistingError, Id] },
      PartialFunction.empty,
      { case u@UserId("1") => Left(UserNotExistingError(u)).asResult[User, Id] },
      PartialFunction.empty,
      Instant.ofEpochSecond(1000),
      UUID.randomUUID()
    )

    val result = bookBike(NumberPlate("1"), UserId("1")).value

    result shouldBe Left(UserNotExistingError(UserId("1")))
  }

  it should "return an error when the bike is already booked" in {
    val bike: Bike = Bike(NumberPlate("1")).change
      .replace(Symbol("userId"), Option(UserId("1")))
      .back[Bike]

    val bookBike = BookingService.bookBike[Id](
      { case NumberPlate("1") => Right(bike).asResult[BikeNotExistingError, Id] },
      PartialFunction.empty,
      { case u@UserId("1") => Right(User(u)).asResult[UserNotExistingError, Id] },
      PartialFunction.empty,
      Instant.ofEpochSecond(1000),
      UUID.randomUUID()
    )

    val result = bookBike(NumberPlate("1"), UserId("1")).value

    result shouldBe Left(BikeAlreadyBookedError(bike))
  }
}