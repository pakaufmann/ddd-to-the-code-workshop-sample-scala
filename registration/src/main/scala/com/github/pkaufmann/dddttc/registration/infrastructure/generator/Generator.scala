package com.github.pkaufmann.dddttc.registration.infrastructure.generator

import java.util.UUID

import cats.effect.Sync
import com.github.pkaufmann.dddttc.domain.UUIDGenerator
import com.github.pkaufmann.dddttc.registration.application.domain.VerificationCode.RandomNumber

import scala.util.Random

object Generator {
  def randomInt[F[_]](implicit S: Sync[F]): RandomNumber[F] = (min, max) => S.delay(Random.between(min, max + 1))

  def uuid[F[_]](implicit S: Sync[F]): UUIDGenerator[F] = S.delay(UUID.randomUUID())
}
