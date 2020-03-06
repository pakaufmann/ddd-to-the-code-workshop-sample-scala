package com.github.pkaufmann.dddttc

import cats.data.ReaderT
import cats.tagless.implicits._
import cats.~>
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import doobie.free.connection.ConnectionIO

package object infrastructure {

  object implicits {
    def conIOToReader[C]: ConnectionIO ~> ReaderT[ConnectionIO, C, *] =
      Lambda[ConnectionIO ~> ReaderT[ConnectionIO, C, *]](a => ReaderT.liftF(a))

    implicit final class ReaderOps[F[_], C, E, R](private val result: Result[ReaderT[F, C, *], E, R]) extends AnyVal {
      def run(tr: C): Result[F, E, R] = {
        result.value.run(tr).asResult
      }
    }
  }

}
