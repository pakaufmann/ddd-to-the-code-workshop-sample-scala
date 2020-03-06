package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

@ValueObject
case class Transaction private(reference: TransactionReference, amount: Amount)
