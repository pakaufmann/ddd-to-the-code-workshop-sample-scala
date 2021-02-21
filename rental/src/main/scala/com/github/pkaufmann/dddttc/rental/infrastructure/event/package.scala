package com.github.pkaufmann.dddttc.rental.infrastructure

import com.github.pkaufmann.dddttc.infrastructure.event.{MqPubSub, MqPublication, MqSubscription, Topic}
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingCompletedEvent
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.rental.infrastructure.json.implicits._
import io.circe.parser.decode
import io.circe.syntax._

package object event {
  object implicits {
    implicit val userRegistrationCompletedMessage = MqSubscription.create[UserRegistrationCompletedMessage](
      Topic("registration/user-registration-completed/Subscriptions/rental"),
      decode[UserRegistrationCompletedMessage](_).toTry
    )

    implicit val bookingCompletedPublication = MqPublication.create[BookingCompletedEvent](
      Topic("rental/booking-completed"),
      _.asJson.noSpaces
    )

    implicit val bookingCompletedSubscription = MqSubscription.create[BookingCompletedEvent](
      Topic("rental/booking-completed/Subscriptions/rental"),
      decode[BookingCompletedEvent](_).toTry
    )
  }
}
