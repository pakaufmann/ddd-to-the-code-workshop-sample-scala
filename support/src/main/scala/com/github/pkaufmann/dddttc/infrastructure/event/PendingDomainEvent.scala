package com.github.pkaufmann.dddttc.infrastructure.event

import java.time.LocalDateTime

case class PendingDomainEvent(id: String, topic: Topic, payload: String, publishedAt: LocalDateTime)
