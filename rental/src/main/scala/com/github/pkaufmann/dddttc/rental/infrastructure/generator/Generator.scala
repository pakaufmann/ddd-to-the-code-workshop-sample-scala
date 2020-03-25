package com.github.pkaufmann.dddttc.rental.infrastructure.generator

import java.util.UUID

import cats.effect.Sync
import com.github.pkaufmann.dddttc.domain.UUIDGenerator

object Generator {
  def uuid[F[_]](implicit S: Sync[F]): UUIDGenerator[F] = S.delay(UUID.randomUUID())
}
