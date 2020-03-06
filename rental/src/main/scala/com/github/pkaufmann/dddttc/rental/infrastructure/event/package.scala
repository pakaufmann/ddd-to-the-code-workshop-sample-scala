package com.github.pkaufmann.dddttc.rental.infrastructure

import com.github.pkaufmann.dddttc.infrastructure.event.{MqPubSub, MqSubscription, Topic}
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingCompletedEvent
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.rental.infrastructure.json.implicits._
import io.circe.parser.decode
import io.circe.syntax._

package object event {
  object implicits {
    implicit val userRegistrationCompletedMessage = MqSubscription.create[UserRegistrationCompletedMessage](
      Topic("registration/user-registration-completed"),
      decode[UserRegistrationCompletedMessage](_).toTry
    )

    implicit val bookingCompletedEvent = MqPubSub[BookingCompletedEvent](
      Topic("rental/booking-completed"),
      _.asJson.noSpaces,
      decode[BookingCompletedEvent](_).toTry
    )
  }
}
