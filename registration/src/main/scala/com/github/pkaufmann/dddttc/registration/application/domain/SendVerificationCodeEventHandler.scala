package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.domain.events.Subscription
import com.github.pkaufmann.dddttc.stereotypes.DomainEventHandler

@DomainEventHandler
object SendVerificationCodeEventHandler {
  def register[F[_]](subscription: Subscription[F, PhoneNumberVerificationCodeGeneratedEvent], smsNotificationSender: SmsNotificationSender[F]) {
    subscription.on { event =>
      val smsText = "Your verification code is " + event.verificationCode.value
      smsNotificationSender.sendSmsTo(event.phoneNumber, smsText)
    }
  }
}