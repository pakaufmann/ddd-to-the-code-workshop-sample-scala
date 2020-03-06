package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.stereotypes.DomainEventHandler

@DomainEventHandler
object SendVerificationCodeEventHandler {
  def onPhoneNumberVerified[F[_]](smsNotificationSender: SmsNotificationSender[F]): Subscription[F, PhoneNumberVerificationCodeGeneratedEvent] = {
    event => {
      val smsText = "Your verification code is " + event.verificationCode.value
      smsNotificationSender.sendSmsTo(event.phoneNumber, smsText)
    }
  }
}