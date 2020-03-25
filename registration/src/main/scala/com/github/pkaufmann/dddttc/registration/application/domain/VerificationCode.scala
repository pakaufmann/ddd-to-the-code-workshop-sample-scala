package com.github.pkaufmann.dddttc.registration.application.domain

import cats.Monad
import cats.implicits._
import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class VerificationCode(value: String) extends AnyVal {
  def matches(code: VerificationCode) = value == code.value

}

object VerificationCode {
  type Min = Int
  type Max = Int

  type RandomNumber[F[_]] = (Min, Max) => F[Int]

  private[application] def random[F[_] : Monad](random: RandomNumber[F]): F[VerificationCode] = {
    random(100000, 999999).map(n => VerificationCode(n.toString))
  }
}
