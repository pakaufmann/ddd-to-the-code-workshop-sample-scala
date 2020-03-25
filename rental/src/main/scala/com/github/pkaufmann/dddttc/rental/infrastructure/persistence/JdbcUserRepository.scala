package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.rental.application.domain.user.{User, UserRepository}
import com.github.pkaufmann.dddttc.rental.application.domain.{UserAlreadyExistsError, UserNotExistingError}
import doobie._
import doobie.implicits._

object JdbcUserRepository {
  val add: UserRepository.Add[ConnectionIO] = {
    user => {
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
  }

  val get: UserRepository.Get[ConnectionIO] = {
    userId => {
      sql"SELECT data FROM user WHERE id = $userId"
        .query[User]
        .option
        .map(_.toRight(UserNotExistingError(userId)))
        .asResult
    }
  }
}
