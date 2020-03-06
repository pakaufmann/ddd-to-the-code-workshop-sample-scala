package com.github.pkaufmann.dddttc.infrastructure.persistence

import com.github.pkaufmann.dddttc.domain.Result

import scala.language.implicitConversions

trait Transaction[T[_], F[_]] {
  def transact[R](in: T[R]): F[R]

  def transact[E, R](in: Result[T, E, R]): Result[F, E, R]
}