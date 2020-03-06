package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class FullName(firstName: String, lastName: String) {
  def firstAndLastName: String = s"$firstName $lastName"
}
