package com.github.pkaufmann.dddttc.rental.application.domain.bike

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class NumberPlate(value: String) extends AnyVal
