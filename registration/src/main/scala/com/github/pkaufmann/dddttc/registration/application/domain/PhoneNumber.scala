package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class PhoneNumber(value: String) extends AnyVal {
  def isSwiss: Boolean = value.startsWith("+41") || value.startsWith("0041")
}
