package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Functor
import cats.Functor.ops._
import com.github.pkaufmann.dddttc.domain.UUIDGenerator
import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class UserRegistrationId(value: String) extends AnyVal

object UserRegistrationId {
  private[domain] def newId[F[_] : Functor](generator: UUIDGenerator[F]): F[UserRegistrationId] =
    generator.map(id => UserRegistrationId(id.toString))
}
