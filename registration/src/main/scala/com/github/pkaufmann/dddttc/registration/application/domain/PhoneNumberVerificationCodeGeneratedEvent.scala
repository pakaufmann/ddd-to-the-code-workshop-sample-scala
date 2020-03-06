package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.DomainEvent

@DomainEvent
case class PhoneNumberVerificationCodeGeneratedEvent private(phoneNumber: PhoneNumber, verificationCode: VerificationCode)

private[domain] object PhoneNumberVerificationCodeGeneratedEvent
