package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class Amount private(value: BigDecimal) extends AnyVal {
  def negate(): Amount = Amount(value.bigDecimal.negate())

  def +(other: Amount): Amount = Amount(this.value + other.value)

  def *(in: Long): Amount = Amount(this.value * in)
}

object Amount {
  def apply(in: String): Amount = Amount(BigDecimal(in))

  def zero: Amount = Amount(BigDecimal("0"))
}
