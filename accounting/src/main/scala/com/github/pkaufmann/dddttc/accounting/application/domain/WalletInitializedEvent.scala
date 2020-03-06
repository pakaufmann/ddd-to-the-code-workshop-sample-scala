package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.stereotypes.DomainEvent

@DomainEvent
case class WalletInitializedEvent private[domain](id: UserId)

private[domain] object WalletInitializedEvent
