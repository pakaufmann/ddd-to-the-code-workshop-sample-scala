package com.github.pkaufmann.dddttc.accounting.infrastructure

import java.time.LocalDateTime

import com.github.pkaufmann.dddttc.accounting.infrastructure.event.BookingCompletedMessageListener.Message.{BikeUsage, BookingCompletedMessage, UserId}
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event.MqSubscription
import io.circe.literal._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class SubscriptionTests extends AnyFlatSpec with Matchers {

  "The booking completed message" should "be correctly decoded" in {
    val message =
      json"""{
       "userId": "user-id-1",
       "bikeUsage": {
          "startedAt": "2020-03-15T10:15:45",
          "duration": 100
       }
    }"""

    val result = MqSubscription[BookingCompletedMessage].asObject(message.noSpaces)

    result.get shouldBe BookingCompletedMessage(
      UserId("user-id-1"),
      BikeUsage(
        LocalDateTime.of(2020, 3, 15, 10, 15, 45),
        100.seconds
      )
    )
  }

  it should "correctly decode the user registration completed message" in {
    val message =
      json"""{
       "userHandle": "user-handle-1"
    }"""

    val result = MqSubscription[UserRegistrationCompletedMessage].asObject(message.noSpaces)

    result.get shouldBe UserRegistrationCompletedMessage("user-handle-1")
  }
}
