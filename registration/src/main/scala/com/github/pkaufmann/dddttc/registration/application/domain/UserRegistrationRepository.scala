package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.domain.Result

trait UserRegistrationRepository[F[_]] {

  def add(userRegistration: UserRegistration): Result[F, UserHandleAlreadyInUseError, Unit]

  def update(verifiedRegistration: UserRegistration): Result[F, UserRegistrationNotExistingError, Unit]

  def find(userHandle: UserHandle): F[Option[UserRegistration]]

  def get(userRegistrationId: UserRegistrationId): Result[F, UserRegistrationNotExistingError, UserRegistration]
}
