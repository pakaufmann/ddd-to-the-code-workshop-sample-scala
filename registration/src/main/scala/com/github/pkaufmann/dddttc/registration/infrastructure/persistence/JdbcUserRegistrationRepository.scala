package com.github.pkaufmann.dddttc.registration.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.application.domain._
import doobie._
import doobie.implicits._

object JdbcUserRegistrationRepository {
  val add: UserRegistrationRepository.Add[ConnectionIO] = {
    userRegistration => {
      sql"INSERT INTO user_registration(id, user_handle, data) VALUES (${userRegistration.id}, ${userRegistration.userHandle}, $userRegistration)"
        .update
        .run
        .attemptSomeSqlState {
          case SqlState(value) if value.contains("23505") =>
            UserHandleAlreadyInUseError(userRegistration.userHandle)
        }
        .asResult
        .map(_ => ())
    }
  }

  val update2: UserRegistrationRepository.Update[ConnectionIO] =
    userRegistration => {
      sql"UPDATE user_registration SET data=$userRegistration WHERE id=${userRegistration.id}"
        .update
        .run
        .map {
          case 0 => Left(UserRegistrationNotExistingError(userRegistration.id))
          case _ => Right()
        }
        .asResult
    }

  val update: UserRegistrationRepository.Update[ConnectionIO] =
    userRegistration => {
      sql"UPDATE user_registration SET data=$userRegistration WHERE id=${userRegistration.id}"
        .update
        .run
        .map {
          case 0 => Left(UserRegistrationNotExistingError(userRegistration.id))
          case _ => Right()
        }
        .asResult
    }

  val find: UserRegistrationRepository.Find[ConnectionIO] = {
    userHandle => {
      sql"SELECT data FROM user_registration WHERE user_handle = $userHandle"
        .query[UserRegistration]
        .option
    }
  }

  val get: UserRegistrationRepository.Get[ConnectionIO] = {
    userRegistrationId => {
      sql"SELECT data FROM user_registration WHERE id = $userRegistrationId"
        .query[UserRegistration]
        .option
        .map(_.toRight(UserRegistrationNotExistingError(userRegistrationId)))
        .asResult
    }
  }
}
