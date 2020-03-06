package com.github.pkaufmann.dddttc.accounting.infrastructure

import com.github.pkaufmann.dddttc.accounting.application.domain._
import io.circe._
import io.circe.generic.semiauto._
import shapeless.LabelledGeneric

import scala.concurrent.duration.FiniteDuration

package object json {
  object implicits {
    implicit val userIdEncoder = Encoder.encodeString.contramap[UserId](_.value)
    implicit val userIdDecoder = Decoder.decodeString.map[UserId](UserId.apply)

    implicit val bookingIdEncoder = Encoder.encodeString.contramap[BookingId](_.value)
    implicit val bookingIdDecoder = Decoder.decodeString.map[BookingId](BookingId.apply)

    implicit val durationEncode = Encoder.encodeLong.contramap[FiniteDuration](_.toSeconds)
    implicit val durationDecoder = Decoder.decodeLong.map[FiniteDuration](FiniteDuration(_, scala.concurrent.duration.SECONDS))

    implicit val labelledWalletInitialized = LabelledGeneric[WalletInitializedEvent]
    implicit val walletInitializedCodec = deriveCodec[WalletInitializedEvent]

    implicit val bookingCoded = deriveCodec[Booking]

    implicit val amountCoded = deriveCodec[Amount]

    implicit val transactionReferenceCodec = deriveCodec[TransactionReference]

    implicit val transactionCodec = deriveCodec[Transaction]

    implicit val labelledWallet = LabelledGeneric[Wallet]
    implicit val walletCodec = deriveCodec[Wallet]
  }
}
