package com.github.pkaufmann.dddttc.registration.application.domain

object SmsNotificationSender {
  type SendSMS[F[_]] = (PhoneNumber, String) => F[Unit]
}
