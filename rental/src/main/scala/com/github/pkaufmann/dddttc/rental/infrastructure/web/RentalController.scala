package com.github.pkaufmann.dddttc.rental.infrastructure.web

import cats.effect.IO
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.Decoders._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.twirl._
import org.http4s.{HttpRoutes, _}

object RentalController {
  def routes[F[_]: IOTransaction](bookService: BookingService[F], bikeService: BikeService[F]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "bikes" =>
        bikeService.listBikes()
          .transact
          .flatMap(bikes => Ok(rental.html.index(bikes)))
      case GET -> Root / "bookings" / "new" :? NumberPlateParameter(numberPlate) =>
        bikeService.getBike(numberPlate)
          .transact
          .foldF(
            {
              case BikeNotExistingError(id) =>
                NotFound(rental.html.error(s"Bike with id ${id.value} does not exist"))
            },
            bike => Ok(rental.html.book(bike))
          )
      case req@POST -> Root / "bookings" =>
        req.decode[BookBikeRequest] { data =>
          bookService.bookBike(data.numberPlate, data.userId)
            .transact
            .foldF(
              {
                case BikeNotExistingError(id) =>
                  NotFound(rental.html.error(s"Bike with id ${id.value} does not exist"))
                case BikeAlreadyBookedError(bike) =>
                  PreconditionFailed(rental.html.error(s"The bike with id ${bike.numberPlate.value} is already booked"))
                case UserNotExistingError(userId) =>
                  NotFound(rental.html.error(s"User '${userId.value} does not exist'"))
              },
              booking => Found(Location(Uri.unsafeFromString(s"/rental/bookings?bookingId=${booking.id.value}")))
            )
        }
      case req@PUT -> Root / "bookings" =>
        req.decode[CompleteBookingRequest] { data =>
          bookService.completeBooking(data.bookingId)
            .transact
            .foldF({
              case BookingAlreadyCompletedError(bookingId) =>
                PreconditionFailed(rental.html.error(s"The booking '${bookingId.value}' was already completed"))
              case BookingNotExistingError(bookingId) =>
                PreconditionFailed(rental.html.error(s"Could not find booking for id '${bookingId.value}'"))
            },
              _ => Found(Location(Uri.unsafeFromString("/rental/bookings")))
            )
        }
      case GET -> Root / "bookings" =>
        bookService.listBookings()
          .transact
          .flatMap(bookings => Ok(rental.html.bookings(bookings)))
    }
  }

}
