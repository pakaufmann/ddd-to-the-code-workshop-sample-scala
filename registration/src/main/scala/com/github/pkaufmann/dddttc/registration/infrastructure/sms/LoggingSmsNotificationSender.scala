package com.github.pkaufmann.dddttc.registration.infrastructure.sms

import cats.effect.Sync
import com.github.pkaufmann.dddttc.registration.application.domain.{PhoneNumber, SmsNotificationSender}

class LoggingSmsNotificationSender[F[_]: Sync]() extends SmsNotificationSender[F] {
  override def sendSmsTo(phoneNumber: PhoneNumber, smsText: String): F[Unit] = {
    Sync[F].delay(println(s"Send sms to $phoneNumber with text: $smsText"))
  }
}