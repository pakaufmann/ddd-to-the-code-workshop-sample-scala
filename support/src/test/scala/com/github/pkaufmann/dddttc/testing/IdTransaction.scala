package com.github.pkaufmann.dddttc.testing

import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.infrastructure.persistence.Transaction

class IdTransaction[F[_]] extends Transaction[F, F] {
  override def transact[R](in: F[R]): F[R] = in

  override def transact[E, R](in: Result[F, E, R]): Result[F, E, R] = in
}
