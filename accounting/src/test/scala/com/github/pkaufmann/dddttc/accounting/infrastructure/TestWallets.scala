package com.github.pkaufmann.dddttc.accounting.infrastructure

import cats.kernel.Eq
import com.github.pkaufmann.dddttc.accounting.application.domain.{Amount, Transaction, UserId, Wallet}
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import shapeless.{HNil, LabelledGeneric}

object TestWallets {
  implicit val fullEq = Eq[Wallet]((x, y) => LabelledGeneric[Wallet].to(x) == LabelledGeneric[Wallet].to(y))

  val default: Wallet = create[Wallet].apply(UserId("1") :: List.empty[Transaction] :: Amount.zero :: HNil)

  val change = default.change
}
