package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class TransactionReference private(value: String) extends AnyVal
