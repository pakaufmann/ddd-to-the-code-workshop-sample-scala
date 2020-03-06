package com.github.pkaufmann.dddttc.registration.infrastructure.persistence

import cats.tagless.Derive
import cats.tagless.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import com.github.pkaufmann.dddttc.registration.application.domain._
import doobie._
import doobie.implicits._

object JdbcUserRegistrationRepository extends UserRegistrationRepository[ConnectionIO] {
  override def add(userRegistration: UserRegistration): Result[ConnectionIO, UserHandleAlreadyInUseError, Unit] = {
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

  override def update(userRegistration: UserRegistration): Result[ConnectionIO, UserRegistrationNotExistingError, Unit] = {
    sql"UPDATE user_registration SET data=$userRegistration WHERE id=${userRegistration.id}"
      .update
      .run
      .map {
        case 0 => Left(UserRegistrationNotExistingError(userRegistration.id))
        case _ => Right()
      }
      .asResult
  }

  override def find(userHandle: UserHandle): ConnectionIO[Option[UserRegistration]] = {
    sql"SELECT data FROM user_registration WHERE user_handle = $userHandle"
      .query[UserRegistration]
      .option
  }

  override def get(userRegistrationId: UserRegistrationId): Result[ConnectionIO, UserRegistrationNotExistingError, UserRegistration] = {
    sql"SELECT data FROM user_registration WHERE id = $userRegistrationId"
      .query[UserRegistration]
      .option
      .map(_.toRight(UserRegistrationNotExistingError(userRegistrationId)))
      .asResult
  }
}
