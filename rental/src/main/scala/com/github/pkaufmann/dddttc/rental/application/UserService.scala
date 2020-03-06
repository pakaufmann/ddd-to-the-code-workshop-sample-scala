package com.github.pkaufmann.dddttc.rental.application

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.UserAlreadyExistsError
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
trait UserService[F[_]] {
  def addUser(id: UserId): Result[F, UserAlreadyExistsError, Unit]
}

object UserService {

  def apply[F[_]](userRepository: UserRepository[F]): UserService[F] = new Impl[F](userRepository)

  private class Impl[F[_]](userRepository: UserRepository[F]) extends UserService[F] {
    def addUser(id: UserId): Result[F, UserAlreadyExistsError, Unit] = {
      userRepository.add(User(id))
    }
  }

}