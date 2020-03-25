package com.github.pkaufmann.dddttc.rental.infrastructure.web

import cats.effect.Sync
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.Decoders._
import org.http4s.dsl.io._
import org.http4s.headers.{Location, `Content-Length`}
import org.http4s.twirl._
import cats.implicits._
import org.http4s.{HttpRoutes, _}

object RentalController {
  def listBikes[F[_] : Sync](listBikes: BikeService.ListBikes[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "bikes" =>
        listBikes
          .map(bikes => Response(status = Status.Ok).withEntity(rental.html.index(bikes)))
    }

  def getBike[F[_] : Sync](getBike: BikeService.GetBike[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "bookings" / "new" :? NumberPlateParameter(numberPlate) =>
        getBike(numberPlate)
          .fold(
            {
              case BikeNotExistingError(id) =>
                Response(status = Status.NotFound)
                  .withEntity(rental.html.error(s"Bike with id ${id.value} does not exist"))
            },
            bike =>
              Response(status = Status.Ok)
                .withEntity(rental.html.book(bike))
          )
    }

  def bookBike[F[_] : Sync](bookBike: BookingService.BookBike[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req@POST -> Root / "bookings" =>
        req.decode[BookBikeRequest] { data =>
          bookBike(data.numberPlate, data.userId)
            .fold(
              {
                case BikeNotExistingError(id) =>
                  Response(status = Status.NotFound)
                    .withEntity(rental.html.error(s"Bike with id ${id.value} does not exist"))
                case BikeAlreadyBookedError(bike) =>
                  Response(status = Status.PreconditionFailed)
                    .withEntity(rental.html.error(s"The bike with id ${bike.numberPlate.value} is already booked"))
                case UserNotExistingError(userId) =>
                  Response(status = Status.NotFound)
                    .withEntity(rental.html.error(s"User '${userId.value} does not exist'"))
              },
              booking =>
                Response(
                  status = Status.Found,
                  headers = Headers(
                    List(
                      `Content-Length`.zero,
                      Location(Uri.unsafeFromString(s"/rental/bookings?bookingId=${booking.id.value}"))
                    )
                  )
                )
            )
        }
    }

  def completeBooking[F[_] : Sync](completeBooking: BookingService.CompleteBooking[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req@PUT -> Root / "bookings" =>
        req.decode[CompleteBookingRequest] { data =>
          completeBooking(data.bookingId)
            .fold({
              case BookingAlreadyCompletedError(bookingId) =>
                Response(status = Status.PreconditionFailed)
                  .withEntity(rental.html.error(s"The booking '${bookingId.value}' was already completed"))
              case BookingNotExistingError(bookingId) =>
                Response(status = Status.PreconditionFailed)
                  .withEntity(rental.html.error(s"Could not find booking for id '${bookingId.value}'"))
            },
              _ =>
                Response(
                  status = Status.Found,
                  headers = Headers(
                    List(
                      `Content-Length`.zero,
                      Location(Uri.unsafeFromString("/rental/bookings"))
                    )
                  )
                )
            )
        }
    }

  def listBookings[F[_] : Sync](listBookings: BookingService.ListBookings[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "bookings" =>
        listBookings
          .map(bookings => Response(status = Status.Ok).withEntity(rental.html.bookings(bookings)))
    }
  }

}
