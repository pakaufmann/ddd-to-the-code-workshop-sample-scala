package com.github.pkaufmann.dddttc.rental.application.domain.booking

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.github.pkaufmann.dddttc.stereotypes.ValueObject

import scala.concurrent.duration._

@ValueObject
case class BikeUsage private(startedAt: LocalDateTime, endedAt: LocalDateTime, duration: FiniteDuration)

object BikeUsage {
  private[domain] def apply(startedAt: LocalDateTime, endedAt: LocalDateTime): BikeUsage = {
    BikeUsage(startedAt, endedAt, startedAt.until(endedAt, ChronoUnit.SECONDS).seconds)
  }
}