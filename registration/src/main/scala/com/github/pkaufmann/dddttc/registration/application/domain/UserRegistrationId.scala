package com.github.pkaufmann.dddttc.registration.application.domain

import java.util.UUID

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class UserRegistrationId(value: String) extends AnyVal

object UserRegistrationId {
  private[domain] def newId(): UserRegistrationId = UserRegistrationId(UUID.randomUUID().toString)
}
