package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.stereotypes.DomainEventHandler

@DomainEventHandler
object SendVerificationCodeEventHandler {
  def onPhoneNumberVerified[F[_]]
  (
    sendSMS: SmsNotificationSender.SendSMS[F]
  ): Subscription[F, PhoneNumberVerificationCodeGeneratedEvent] = {
    event => {
      sendSMS(event.phoneNumber, "Your verification code is " + event.verificationCode.value)
    }
  }
}