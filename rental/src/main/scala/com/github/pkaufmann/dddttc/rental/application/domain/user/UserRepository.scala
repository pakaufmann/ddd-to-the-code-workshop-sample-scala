package com.github.pkaufmann.dddttc.rental.application.domain.user

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.{UserAlreadyExistsError, UserNotExistingError}
import org.springframework.stereotype.Repository

@Repository
object UserRepository {
  type Add[F[_]] = User => Result[F, UserAlreadyExistsError, Unit]

  type Get[F[_]] = UserId => Result[F, UserNotExistingError, User]
}
