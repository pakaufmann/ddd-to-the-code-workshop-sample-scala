package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Id
import com.github.pkaufmann.dddttc.testing.TestSubscription
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SendVerificationCodeEventHandlerTests extends AnyFlatSpec with Matchers with MockFactory {
  val smsNotificationSender = mock[SmsNotificationSender[Id]]
  val subscription = new TestSubscription[PhoneNumberVerificationCodeGeneratedEvent]()

  "The event handler" should "send an sms when it receives a phone number verified event" in {
    val number = PhoneNumber("+41 79 123 45 67")

    SendVerificationCodeEventHandler.register(subscription, smsNotificationSender)

    (smsNotificationSender.sendSmsTo _).expects(number, argThat((s: String) => s.contains("123456"))) returning()

    subscription.send(PhoneNumberVerificationCodeGeneratedEvent(number, VerificationCode("123456")))
  }
}
