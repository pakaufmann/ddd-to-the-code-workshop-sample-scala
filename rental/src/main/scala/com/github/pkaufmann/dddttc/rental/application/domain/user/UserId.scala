package com.github.pkaufmann.dddttc.rental.application.domain.user

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class UserId(value: String) extends AnyVal
