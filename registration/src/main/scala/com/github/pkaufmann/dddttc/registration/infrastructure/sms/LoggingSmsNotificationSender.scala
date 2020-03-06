package com.github.pkaufmann.dddttc.registration.infrastructure.sms

import cats.data.ReaderT
import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.registration.application.domain.{PhoneNumber, SmsNotificationSender}

class LoggingSmsNotificationSender[F[_] : Sync]() extends SmsNotificationSender[ReaderT[F, Trace, *]] {
  override def sendSmsTo(phoneNumber: PhoneNumber, smsText: String): ReaderT[F, Trace, Unit] =
    ReaderT { trace: Trace =>
      Sync[F].delay(println(s"Send sms to $phoneNumber with text: $smsText and $trace"))
    }
}