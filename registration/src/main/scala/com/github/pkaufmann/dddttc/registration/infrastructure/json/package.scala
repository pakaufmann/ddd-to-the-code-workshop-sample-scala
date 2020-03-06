package com.github.pkaufmann.dddttc.registration.infrastructure

import com.github.pkaufmann.dddttc.registration.application.domain._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import shapeless.LabelledGeneric

package object json {

  object implicits {
    implicit val userRegistrationIdEncoder = Encoder.encodeString.contramap[UserRegistrationId](_.value)
    implicit val userRegistrationIdDecoder = Decoder.decodeString.map[UserRegistrationId](UserRegistrationId.apply)

    implicit val userHandleEncoder = Encoder.encodeString.contramap[UserHandle](_.value)
    implicit val userHandleDecoder = Decoder.decodeString.map[UserHandle](UserHandle.apply)

    implicit val phoneNumberEncoder = Encoder.encodeString.contramap[PhoneNumber](_.value)
    implicit val phoneNumberDecoder = Decoder.decodeString.map[PhoneNumber](PhoneNumber.apply)

    implicit val verificationCodeEncoder = Encoder.encodeString.contramap[VerificationCode](_.value)
    implicit val verificationCodeDecoder = Decoder.decodeString.map[VerificationCode](VerificationCode.apply)

    implicit val phoneNumberCodec = deriveCodec[PhoneNumber]

    implicit val verificationCodeCodec = deriveCodec[VerificationCode]

    implicit val fullNameCodec = deriveCodec[FullName]

    implicit val labelledPhoneNumberVerificationCodeGenerated = LabelledGeneric[PhoneNumberVerificationCodeGeneratedEvent]
    implicit val phoneNumberVerificationCodeGeneratedCodec = deriveCodec[PhoneNumberVerificationCodeGeneratedEvent]

    implicit val labelledUserRegistrationCompleted = LabelledGeneric[UserRegistrationCompletedEvent]
    implicit val userRegistrationCompletedCodec = deriveCodec[UserRegistrationCompletedEvent]

    implicit val labelledUserRegistration = LabelledGeneric[UserRegistration]
    implicit val userRegistrationCodec = deriveCodec[UserRegistration]
  }

}
