package com.github.pkaufmann.dddttc.infrastructure.event

import java.time.LocalDateTime

import com.github.pkaufmann.dddttc.infrastructure.Trace

case class PendingDomainEvent(id: String, topic: Topic, payload: String, publishedAt: LocalDateTime, trace: Trace)
