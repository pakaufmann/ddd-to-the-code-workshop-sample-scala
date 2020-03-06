package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserId, UserRepository}
import com.github.pkaufmann.dddttc.rental.application.domain.{UserAlreadyExistsError, UserNotExistingError}
import doobie._
import doobie.implicits._

class JdbcUserRepository extends UserRepository[ConnectionIO] {
  override def add(user: User): Result[ConnectionIO, UserAlreadyExistsError, Unit] = {
    sql"INSERT INTO user(id, data) VALUES(${user.id}, $user)"
      .update
      .run
      .attemptSomeSqlState {
        case SqlState(value) if value.contains("23505") =>
          UserAlreadyExistsError(user.id)
      }
      .asResult
      .map(_ => ())
  }

  override def get(userId: UserId): Result[ConnectionIO, UserNotExistingError, User] = {
    sql"SELECT data FROM user WHERE id = $userId"
      .query[User]
      .option
      .map(_.toRight(UserNotExistingError(userId)))
      .asResult
  }
}
