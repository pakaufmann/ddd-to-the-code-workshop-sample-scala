package com.github.pkaufmann.dddttc.accounting.infrastructure.event

import java.time.LocalDateTime

import cats.Functor
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.BookingCompletedMessageListener.Message.BookingCompletedMessage
import com.github.pkaufmann.dddttc.domain.Subscription
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.log4s._

import scala.concurrent.duration._

object BookingCompletedMessageListener {
  private val logger = getLogger

  def apply[F[_] : Functor](billedBookingFee: WalletService.BillBookingFee[F]): Subscription[F, BookingCompletedMessage] =
    message => {
      billedBookingFee(Booking(UserId(message.userId.value), message.bikeUsage.startedAt, message.bikeUsage.duration))
        .fold(
          {
            case WalletNotExistingError(userId) =>
              logger.info(s"Could not find wallet for user: $userId")
            case BookingAlreadyBilled(wallet, booking) =>
              logger.info(s"Booking was already billed in wallet: wallet id=${wallet.id}, booking=${booking.id}")
          },
          _ => ()
        )
    }

  object Message {

    case class UserId(value: String)

    case class BikeUsage(startedAt: LocalDateTime, duration: FiniteDuration)

    case class BookingCompletedMessage(userId: UserId, bikeUsage: BikeUsage)

    implicit val userIdDecoder = Decoder.decodeString.map[UserId](UserId.apply)

    implicit val finiteDurationDecoder = Decoder.decodeLong.map(_.seconds)

    implicit val bikeUsageDecoder = deriveDecoder[BikeUsage]

    implicit val bookingCompletedMessageDecoder = deriveDecoder[BookingCompletedMessage]
  }

}
