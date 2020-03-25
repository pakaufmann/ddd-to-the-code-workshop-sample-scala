package com.github.pkaufmann.dddttc

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import cats.{Applicative, Bifunctor, Functor}

import scala.language.implicitConversions

package object domain {
  type Instant[F[_]] = F[java.time.Instant]

  type Subscription[F[_], T] = T => F[Unit]

  type UUIDGenerator[F[_]] = F[UUID]

  type Publisher[F[_], T] = T => F[Unit]

  type Result[F[_], E, R] = EitherT[F, E, R]

  object implicits {

    implicit final class RightEffectOps[+E, R](private val right: Right[E, R]) extends AnyVal {
      def asResult[E1 >: E, F[_] : Applicative]: Result[F, E1, R] = right.withLeft[E1].toEitherT[F]
    }

    implicit final class LeftEffectOps[E, +R](private val left: Left[E, R]) extends AnyVal {
      def asResult[R1 >: R, F[_] : Applicative]: Result[F, E, R1] = left.withRight[R1].toEitherT[F]
    }

    implicit final class EitherOps[E, R](private val result: Either[E, R]) extends AnyVal {
      def asResult[F[_] : Applicative]: Result[F, E, R] = result.toEitherT[F]
    }

    implicit final class TOps[T](private val result: T) extends AnyVal {
      def asErrorResult[E >: T, R, F[_] : Applicative]: Result[F, E, R] = result.asLeft[R].leftWiden[E].toEitherT[F]

      def asSuccessResult[E, F[_] : Applicative] = result.asRight[E].toEitherT[F]
    }

    implicit final class EffectOps[R, F[_] : Functor](private val e: F[R]) {
      def asResult[E]: Result[F, E, R] = EitherT.right[E](e)
    }

    implicit final class EitherEffectOps[F[_], E, R](private val e: F[Either[E, R]]) extends AnyVal {
      def asResult: Result[F, E, R] = EitherT(e)

      def asResult[E1 >: E](implicit A: Applicative[F]): Result[F, E1, R] = EitherT(e).leftWiden[E1]
    }

  }

}
