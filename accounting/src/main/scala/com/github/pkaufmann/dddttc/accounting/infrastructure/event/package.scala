package com.github.pkaufmann.dddttc.accounting.infrastructure

import com.github.pkaufmann.dddttc.accounting.application.domain.WalletInitializedEvent
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.BookingCompletedMessageListener.Message.BookingCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.json.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event._
import io.circe.parser.decode
import io.circe.syntax._

package object event {

  object implicits {
    implicit val walletInitializedPublication = MqPublication.create[WalletInitializedEvent](
      Topic("accounting/wallet-initialized"),
      _.asJson.noSpaces
    )

    implicit val bookingCompletedMessageSubscription = MqSubscription.create[BookingCompletedMessage](
      Topic("rental/booking-completed"),
      decode[BookingCompletedMessage](_).toTry
    )

    implicit val userRegistrationCompletedSubscription = MqSubscription.create[UserRegistrationCompletedMessage](
      Topic("registration/user-registration-completed"),
      decode[UserRegistrationCompletedMessage](_).toTry
    )
  }

}