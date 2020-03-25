package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Id
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SendVerificationCodeEventHandlerTests extends AnyFlatSpec with Matchers {
  "The event handler" should "send an sms when it receives a phone number verified event" in {
    val number = PhoneNumber("+41 79 123 45 67")
    val textMatcher = ".*123456.*".r

    val subscription = SendVerificationCodeEventHandler.onPhoneNumberVerified[Id](
      { case (`number`, textMatcher(_*)) => () }
    )

    val result = subscription(PhoneNumberVerificationCodeGeneratedEvent(number, VerificationCode("123456")))
    result shouldBe()
  }
}
