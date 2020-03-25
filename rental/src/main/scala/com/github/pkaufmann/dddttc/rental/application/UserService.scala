package com.github.pkaufmann.dddttc.rental.application

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.rental.application.domain.UserAlreadyExistsError
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId, UserRepository}
import com.github.pkaufmann.dddttc.stereotypes.ApplicationService

@ApplicationService
object UserService {
  type AddUser[F[_]] = UserId => Result[F, UserAlreadyExistsError, Unit]

  def addUser[F[_]](addUser: UserRepository.Add[F]): AddUser[F] = addUser.compose(User.apply)
}
