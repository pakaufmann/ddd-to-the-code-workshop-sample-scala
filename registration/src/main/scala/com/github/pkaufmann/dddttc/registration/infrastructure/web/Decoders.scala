package com.github.pkaufmann.dddttc.registration.infrastructure.web

import cats.effect.IO
import com.github.pkaufmann.dddttc.infrastructure.web.UrlFormDecoder
import com.github.pkaufmann.dddttc.registration.application.domain._

object Decoders {

  case class NewUserRegistrationRequest(userHandle: UserHandle, phoneNumber: PhoneNumber)

  case class VerifyPhoneNumberRequest(userRegistrationId: UserRegistrationId, verificationCode: VerificationCode, userHandle: UserHandle)

  case class CompleteRegistrationRequest(userRegistrationId: UserRegistrationId, firstName: String, lastName: String, userHandle: UserHandle)

  implicit val userHandle = UrlFormDecoder.createDecoder[UserHandle](UserHandle.apply)
  implicit val phone = UrlFormDecoder.createDecoder[PhoneNumber](PhoneNumber.apply)
  implicit val userRegistrationId = UrlFormDecoder.createDecoder[UserRegistrationId](UserRegistrationId.apply)
  implicit val verificationCode = UrlFormDecoder.createDecoder[VerificationCode](VerificationCode.apply)

  implicit val newUserRegistrationDecoder = UrlFormDecoder[IO, NewUserRegistrationRequest]

  implicit val verifyPhoneNumberDecoder = UrlFormDecoder[IO, VerifyPhoneNumberRequest]

  implicit val completeRegistrationDecoder = UrlFormDecoder[IO, CompleteRegistrationRequest]
}
