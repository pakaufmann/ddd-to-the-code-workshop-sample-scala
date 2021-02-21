package com.github.pkaufmann.dddttc.registration.infrastructure

import com.github.pkaufmann.dddttc.infrastructure.event.{MqPubSub, MqPublication, MqSubscription, Topic}
import com.github.pkaufmann.dddttc.registration.application.domain.{PhoneNumberVerificationCodeGeneratedEvent, UserRegistrationCompletedEvent}
import com.github.pkaufmann.dddttc.registration.infrastructure.json.implicits._
import io.circe.parser.decode
import io.circe.syntax._

package object event {

  object implicits {
    implicit val phoneNumberVerificationCodeGeneratedSubscription = MqSubscription.create[PhoneNumberVerificationCodeGeneratedEvent](
      Topic("registration/phone-number-verification-code-generated/Subscriptions/registration"),
      decode[PhoneNumberVerificationCodeGeneratedEvent](_).toTry
    )

    implicit val phoneNumberVerificationCodeGeneratedPublication = MqPublication.create[PhoneNumberVerificationCodeGeneratedEvent](
      Topic("registration/phone-number-verification-code-generated"),
      _.asJson.noSpaces
    )

    implicit val userRegistrationCompletedEvent = MqPublication.create[UserRegistrationCompletedEvent](
      Topic("registration/user-registration-completed"),
      _.asJson.noSpaces
    )
  }

}
