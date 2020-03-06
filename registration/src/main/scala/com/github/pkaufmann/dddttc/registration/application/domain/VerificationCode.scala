package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

import scala.util.Random

@ValueObject
case class VerificationCode(value: String) extends AnyVal {
  def matches(code: VerificationCode) = value == code.value

}

object VerificationCode {
  private[domain] def random(): VerificationCode = {
    VerificationCode(LazyList
      .continually(Random.nextInt(10))
      .take(6)
      .mkString(""))
  }
}
