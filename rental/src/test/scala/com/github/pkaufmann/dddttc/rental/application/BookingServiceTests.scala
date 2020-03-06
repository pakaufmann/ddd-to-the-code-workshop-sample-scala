package com.github.pkaufmann.dddttc.rental.application

import java.time._

import cats.implicits._
import cats.{Id, _}
import com.github.pkaufmann.dddttc.domain.events.Publisher
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.{Bike, BikeRepository, NumberPlate}
import com.github.pkaufmann.dddttc.rental.application.domain.booking._
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId, UserRepository}
import com.github.pkaufmann.dddttc.rental.application.domain.{BikeAlreadyBookedError, BikeNotExistingError, UserNotExistingError}
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class BookingServiceTests extends AnyFlatSpec with MockFactory with Matchers {
  val bookingRepository = mock[BookingRepository[Id]]
  val userRepository = mock[UserRepository[Id]]
  val bikeRepository = mock[BikeRepository[Id]]

  val domainPublisher = mock[Publisher[Id, BookingCompletedEvent]]
  implicit val clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneId.of("UTC"))

  val bookBikeService = new BookBikeService[Id](bikeRepository, bookingRepository, userRepository, clock)
  val bookService = new BookingService(bookBikeService, bookingRepository, domainPublisher, clock)

  "The booking service" should "create a new booking" in {
    val bookedBike: Bike = Bike(NumberPlate("1")).change
      .replace(Symbol("userId"), Option(UserId("2")))
      .back[Bike]

    val booking: Booking = Booking(NumberPlate("1"), UserId("1")).change
      .replace(Symbol("id"), BookingId("fake"))
      .back[Booking]

    (bikeRepository.get _).expects(NumberPlate("1")) returning Right(Bike(NumberPlate("1"))).asResult[BikeNotExistingError, Id]
    (userRepository.get _).expects(UserId("1")) returning Right(User(UserId("1"))).asResult[UserNotExistingError, Id]
    (bikeRepository.update _).expects(bookedBike) returning()
    (bookingRepository.add _).expects(whereEqv(booking)) returning()

    val result = bookService.bookBike(NumberPlate("1"), UserId("1"))

    result.getOrElse(fail()).eqv(booking) shouldBe true
  }

  it should "return an error when the user could not be found" in {
    (bikeRepository.get _).expects(NumberPlate("1")) returning Right(Bike(NumberPlate("1"))).asResult[BikeNotExistingError, Id]
    (userRepository.get _).expects(UserId("1")) returning Left(UserNotExistingError(UserId("1"))).asResult[User, Id]

    val result = bookService.bookBike(NumberPlate("1"), UserId("1")).value

    result shouldBe Left(UserNotExistingError(UserId("1")))
  }

  it should "return an error when the bike is already booked" in {
    val bike: Bike = Bike(NumberPlate("1")).change
      .replace(Symbol("userId"), Option(UserId("1")))
      .back[Bike]

    (bikeRepository.get _).expects(NumberPlate("1")) returning Right(bike).asResult[BikeNotExistingError, Id]
    (userRepository.get _).expects(UserId("1")) returning Right(User(UserId("1"))).asResult[UserNotExistingError, Id]

    val result = bookService.bookBike(NumberPlate("1"), UserId("1")).value

    result shouldBe Left(BikeAlreadyBookedError(bike))
  }
}