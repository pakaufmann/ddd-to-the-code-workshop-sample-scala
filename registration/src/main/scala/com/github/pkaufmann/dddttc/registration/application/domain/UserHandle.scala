package com.github.pkaufmann.dddttc.registration.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class UserHandle(value: String) extends AnyVal
