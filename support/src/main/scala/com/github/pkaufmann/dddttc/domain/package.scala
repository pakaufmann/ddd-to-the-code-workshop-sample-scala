package com.github.pkaufmann.dddttc

import cats.data.EitherT
import cats.implicits._
import cats.{Applicative, Functor}

import scala.language.implicitConversions

package object domain {
  type Result[F[_], E, R] = EitherT[F, E, R]

  case object Result {
    def apply[F[_], E, R](in: F[Either[E, R]]): Result[F, E, R] = EitherT(in)
  }

  object implicits {

    implicit final class EitherOps[E, R](private val result: Either[E, R]) extends AnyVal {
      def asResult[F[_] : Applicative]: Result[F, E, R] = result.toEitherT[F]
    }

    implicit final class RightEffectOps[+E, R](private val right: Right[E, R]) extends AnyVal {
      def asResult[E1 >: E, F[_] : Applicative]: Result[F, E1, R] = right.withLeft[E1].toEitherT[F]
    }

    implicit final class LeftEffectOps[E, +R](private val left: Left[E, R]) extends AnyVal {
      def asResult[R1 >: R, F[_] : Applicative]: Result[F, E, R1] = left.withRight[R1].toEitherT[F]
    }

    implicit final class EffectOps[R, F[_] : Functor](private val e: F[R]) {
      def asResult[E]: Result[F, E, R] = EitherT.right[E](e)
    }

    implicit final class EitherEffectOps[F[_], E, R](private val e: F[Either[E, R]]) extends AnyVal {
      def asResult: Result[F, E, R] = Result(e)
    }

  }

}
