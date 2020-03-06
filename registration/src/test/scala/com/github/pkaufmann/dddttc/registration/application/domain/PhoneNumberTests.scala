package com.github.pkaufmann.dddttc.registration.application.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PhoneNumberTests extends AnyFlatSpec with Matchers {
  "The phone number " should "be swiss for the +41 prefix" in {
    PhoneNumber("+41791234567").isSwiss shouldBe true
  }

  it should "be swiss for the 0041 prefix" in {
    PhoneNumber("0041791234567").isSwiss shouldBe true
  }

  it should "return false for any other prefix" in {
    PhoneNumber("+43791234567").isSwiss shouldBe false
  }
}
