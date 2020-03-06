package com.github.pkaufmann.dddttc.registration

import cats.Eq
import com.github.pkaufmann.dddttc.registration.application.domain._
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import shapeless.record._
import shapeless.{HNil, LabelledGeneric}

object TestRegistrations {
  val noIdEq = Eq.instance[UserRegistration]((x, y) => LabelledGeneric[UserRegistration].to(x).remove(Symbol("id"))._2 == LabelledGeneric[UserRegistration].to(y).remove(Symbol("id"))._2)

  implicit val fullEq = Eq.instance[UserRegistration]((x, y) => LabelledGeneric[UserRegistration].to(x) == LabelledGeneric[UserRegistration].to(y))

  def default: UserRegistration = create[UserRegistration].apply(UserRegistrationId("user-registration-id-1") :: UserHandle("peter") :: PhoneNumber("+41 79 123 45 67") :: VerificationCode("123456") :: None :: false :: false :: HNil)

  def completed: UserRegistration = default.change
    .replace(Symbol("phoneNumberVerified"), true)
    .replace(Symbol("completed"), true)
    .replace(Symbol("fullName"), Option(FullName("Peter", "Smith")))
    .back[UserRegistration]
}
