package com.github.pkaufmann.dddttc.registration.application.domain

trait SmsNotificationSender[F[_]] {
  def sendSmsTo(phoneNumber: PhoneNumber, smsText: String): F[Unit]
}
