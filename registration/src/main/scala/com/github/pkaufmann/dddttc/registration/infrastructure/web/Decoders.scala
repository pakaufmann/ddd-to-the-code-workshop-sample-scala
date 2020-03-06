package com.github.pkaufmann.dddttc.registration.infrastructure.web

import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.Trace
import com.github.pkaufmann.dddttc.infrastructure.web.UrlFormDecoder
import com.github.pkaufmann.dddttc.registration.application.domain._

object Decoders {

  case class NewUserRegistrationRequest(userHandle: UserHandle, phoneNumber: PhoneNumber, trace: Option[Trace])

  case class VerifyPhoneNumberRequest(userRegistrationId: UserRegistrationId, verificationCode: VerificationCode, userHandle: UserHandle, trace: Trace)

  case class CompleteRegistrationRequest(userRegistrationId: UserRegistrationId, firstName: String, lastName: String, userHandle: UserHandle, trace: Trace)

  implicit val userHandle = UrlFormDecoder.createDecoder[UserHandle](UserHandle.apply)
  implicit val phone = UrlFormDecoder.createDecoder[PhoneNumber](PhoneNumber.apply)
  implicit val userRegistrationId = UrlFormDecoder.createDecoder[UserRegistrationId](UserRegistrationId.apply)
  implicit val verificationCode = UrlFormDecoder.createDecoder[VerificationCode](VerificationCode.apply)
  implicit val trace = UrlFormDecoder.createDecoder[Trace](Trace.apply)

  implicit def newUserRegistrationDecoder[F[_] : Sync] = UrlFormDecoder[F, NewUserRegistrationRequest]

  implicit def verifyPhoneNumberDecoder[F[_] : Sync] = UrlFormDecoder[F, VerifyPhoneNumberRequest]

  implicit def completeRegistrationDecoder[F[_] : Sync] = UrlFormDecoder[F, CompleteRegistrationRequest]
}
