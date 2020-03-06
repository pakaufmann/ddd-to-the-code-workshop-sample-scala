package com.github.pkaufmann.dddttc.rental.application.domain.user

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.{UserAlreadyExistsError, UserNotExistingError}
import org.springframework.stereotype.Repository

@Repository
trait UserRepository[F[_]] {
  def add(user: User): Result[F, UserAlreadyExistsError, Unit]

  def get(userId: UserId): Result[F, UserNotExistingError, User]
}
