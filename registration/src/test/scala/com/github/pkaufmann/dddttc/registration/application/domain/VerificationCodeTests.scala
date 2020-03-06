package com.github.pkaufmann.dddttc.registration.application.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VerificationCodeTests extends AnyFlatSpec with Matchers {
  "The verification code" should "be 6 digits long" in {
    VerificationCode.random().value should have length 6
  }

  it should "generate nearly unique values" in {
    val toCreate = 10000
    val codes = LazyList.continually(VerificationCode.random()).take(toCreate).toSet

    codes.size shouldBe >(toCreate - 100)
  }
}
