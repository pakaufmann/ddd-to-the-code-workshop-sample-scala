package com.github.pkaufmann.dddttc.registration.application.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UserRegistrationIdTests extends AnyFlatSpec with Matchers {
  "The user registration id" should "generate unique values" in {
    val toCreate = 10000
    val values = LazyList.continually(UserRegistrationId.newId()).take(toCreate).toSet

    values should have size toCreate
  }
}
