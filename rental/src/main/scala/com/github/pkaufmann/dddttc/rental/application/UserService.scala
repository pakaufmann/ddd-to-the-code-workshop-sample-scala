package com.github.pkaufmann.dddttc.rental.application

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.UserAlreadyExistsError
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
class UserService[F[_]](userRepository: UserRepository[F]) {
  def addUser(id: UserId): Result[F, UserAlreadyExistsError, Unit] = {
    userRepository.add(User(id))
  }
}
