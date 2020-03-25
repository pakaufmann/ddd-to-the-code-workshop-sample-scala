package com.github.pkaufmann.dddttc.registration.infrastructure.sms

import cats.data.ReaderT
import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.registration.application.domain.SmsNotificationSender.SendSMS

object LoggingSmsNotificationSender {
  def sendSmsTo[F[_] : Sync]: SendSMS[ReaderT[F, Trace, *]] =
    (phoneNumber, smsText) => ReaderT { trace =>
      Sync[F].delay(println(s"Send sms to $phoneNumber with text: $smsText, and trace: $trace"))
    }
}