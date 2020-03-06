package com.github.pkaufmann.dddttc.rental.infrastructure.web

import cats.effect.Sync
import cats.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain._
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.Decoders._
import doobie.free.connection.ConnectionIO
import org.http4s.dsl.io._
import org.http4s.headers.{Location, `Content-Length`}
import org.http4s.twirl._
import org.http4s.{HttpRoutes, _}
import doobie.implicits._
import doobie.util.transactor.Transactor

object RentalController {
  def routes[F[_] : Sync](bookService: BookingService[ConnectionIO], bikeService: BikeService[ConnectionIO])(implicit xa: Transactor[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "bikes" =>
        bikeService
          .listBikes()
          .transact(xa)
          .map { bikes =>
            Response(status = Status.Ok)
              .withEntity(rental.html.index(bikes))
          }
      case GET -> Root / "bookings" / "new" :? NumberPlateParameter(numberPlate) =>
        bikeService
          .getBike(numberPlate)
          .transact(xa)
          .fold(
            {
              case BikeNotExistingError(id) =>
                Response(status = Status.NotFound)
                  .withEntity(rental.html.error(s"Bike with id ${id.value} does not exist"))
            },
            bike => Response(status = Status.Ok)
              .withEntity(rental.html.book(bike))
          )
      case req@POST -> Root / "bookings" =>
        req.decode[BookBikeRequest] { data =>
          bookService
            .bookBike(data.numberPlate, data.userId)
            .transact(xa)
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
      case req@PUT -> Root / "bookings" =>
        req.decode[CompleteBookingRequest] { data =>
          bookService
            .completeBooking(data.bookingId)
            .transact(xa)
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
      case GET -> Root / "bookings" =>
        bookService
          .listBookings()
          .transact(xa)
          .map(bookings => Response(status = Status.Ok)
            .withEntity(rental.html.bookings(bookings)))
    }
  }

}
