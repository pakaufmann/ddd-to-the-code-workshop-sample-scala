package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Id
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VerificationCodeTests extends AnyFlatSpec with Matchers {
  "The verification code" should "be 6 digits long" in {
    VerificationCode.random[Id](
      { case (100000, 999999) => 123456 }
    ).value should have length 6
  }
}
