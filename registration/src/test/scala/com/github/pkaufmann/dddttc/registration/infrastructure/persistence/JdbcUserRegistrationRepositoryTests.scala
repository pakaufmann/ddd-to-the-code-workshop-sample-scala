package com.github.pkaufmann.dddttc.registration.infrastructure.persistence

import com.github.pkaufmann.dddttc.registration.TestRegistrations
import com.github.pkaufmann.dddttc.registration.TestRegistrations._
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import com.github.pkaufmann.dddttc.testing.DbTest
import doobie.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._

class JdbcUserRegistrationRepositoryTests extends AnyFlatSpec with Matchers with AggregateMatchers with DbTest {
  "The repository" should "store a user registration in the database" in {
    val completedRegistration: UserRegistration = TestRegistrations.completed

    val (count, found) = run {
      for {
        _ <- JdbcUserRegistrationRepository.add(completedRegistration).value
        count <- sql"""SELECT COUNT(*) FROM user_registration""".query[Long].unique
        found <- JdbcUserRegistrationRepository.get(completedRegistration.id).value
      } yield (count, found)
    }

    count shouldBe 1
    found.getOrElse(fail()) should eqv(completedRegistration)
  }

  it should "return an error when an already existing user handle is added again" in {
    val existing = TestRegistrations.default
    val duplicatedHandle: UserRegistration = TestRegistrations.default.change
      .replace(Symbol("id"), UserRegistrationId("duplicated"))
      .back[UserRegistration]

    val error = run {
      for {
        _ <- JdbcUserRegistrationRepository.add(existing).value
        result <- JdbcUserRegistrationRepository.add(duplicatedHandle).value
      } yield result
    }

    error shouldBe Left(UserHandleAlreadyInUseError(UserHandle("peter")))
  }

  it should "update an existing user registration" in {
    val existing = TestRegistrations.completed
    val updated: UserRegistration = existing.change
      .replace(Symbol("completed"), false)
      .replace(Symbol("phoneNumberVerified"), false)
      .back[UserRegistration]

    val (count, result) = run {
      for {
        _ <- JdbcUserRegistrationRepository.add(existing).value
        _ <- JdbcUserRegistrationRepository.update(updated).value
        count <- sql"""SELECT COUNT(*) FROM user_registration""".query[Long].unique
        r <- JdbcUserRegistrationRepository.get(existing.id).value
      } yield (count, r)
    }

    count shouldBe 1
    result.getOrElse(fail()) should eqv(updated)
  }

  it should "return an error if a non-existing user registration is updated" in {
    val nonExisting = TestRegistrations.default

    val error = run(JdbcUserRegistrationRepository.update(nonExisting).value)

    error shouldBe Left(UserRegistrationNotExistingError(nonExisting.id))
  }

  it should "return an error if a non-existing user registration is searched" in {
    val nonExisting = UserRegistrationId("non-existing")

    val error = run(JdbcUserRegistrationRepository.get(nonExisting))

    error shouldBe Left(UserRegistrationNotExistingError(nonExisting))
  }

  it should "return an existing user registration" in {
    val registration = TestRegistrations.default

    val result  = run {
      for {
        _ <- JdbcUserRegistrationRepository.add(registration).value
        r <- JdbcUserRegistrationRepository.find(registration.userHandle)
      } yield r
    }

    result shouldBe defined
    result.get should eqv(registration)
  }

  it should "return nothing when no registration for a handle exists" in {
    val nonExisting = UserHandle("non-existing")

    val notFound = run(JdbcUserRegistrationRepository.find(nonExisting))

    notFound should not be defined
  }
}
