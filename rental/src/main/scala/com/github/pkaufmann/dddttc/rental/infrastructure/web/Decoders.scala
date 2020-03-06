package com.github.pkaufmann.dddttc.rental.infrastructure.web

import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.web.UrlFormDecoder
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingId
import com.github.pkaufmann.dddttc.rental.application.domain.user.UserId
import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.QueryParamDecoderMatcher

object Decoders {
  implicit val numberPlateParamDecoder: QueryParamDecoder[NumberPlate] = QueryParamDecoder[String].map(NumberPlate.apply)

  object NumberPlateParameter extends QueryParamDecoderMatcher[NumberPlate]("numberPlate")

  case class BookBikeRequest(numberPlate: NumberPlate, userId: UserId)

  case class CompleteBookingRequest(bookingId: BookingId)

  implicit val numberPlate = UrlFormDecoder.createDecoder[NumberPlate](NumberPlate.apply)
  implicit val userId = UrlFormDecoder.createDecoder[UserId](UserId.apply)
  implicit val bookingId = UrlFormDecoder.createDecoder[BookingId](BookingId.apply)

  implicit def bookBikeDecoder[F[_] : Sync] = UrlFormDecoder[F, BookBikeRequest]
  implicit def completeBookingDecoder[F[_] : Sync] = UrlFormDecoder[F, CompleteBookingRequest]
}
