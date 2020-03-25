package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.domain.Result

object UserRegistrationRepository {
  type Add[F[_]] = UserRegistration => Result[F, UserHandleAlreadyInUseError, Unit]

  type Update[F[_]] = UserRegistration => Result[F, UserRegistrationNotExistingError, Unit]

  type Find[F[_]] = UserHandle => F[Option[UserRegistration]]

  type Get[F[_]] = UserRegistrationId => Result[F, UserRegistrationNotExistingError, UserRegistration]
}