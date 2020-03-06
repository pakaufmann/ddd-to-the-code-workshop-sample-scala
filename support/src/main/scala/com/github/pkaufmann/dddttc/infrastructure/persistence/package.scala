package com.github.pkaufmann.dddttc.infrastructure

import cats.effect.IO
import com.github.pkaufmann.dddttc.domain.Result

import scala.language.implicitConversions

package object persistence {

  object implicits {
    type IOTransaction[F[_]] = Transaction[F, IO]

    implicit class Transacting[T[_], R](val in: T[R]) extends AnyVal {
      def transact[F[_]](implicit t: Transaction[T, F]): F[R] = t.transact(in)
    }

    implicit class TransactingResult[T[_], E, A](val in: Result[T, E, A]) extends AnyVal {
      def transact[F[_]](implicit t: Transaction[T, F]): Result[F, E, A] = t.transact(in)
    }

  }

}
