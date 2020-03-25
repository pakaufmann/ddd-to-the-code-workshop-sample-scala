package com.github.pkaufmann.dddttc

import java.util.concurrent.TimeUnit

import cats.MonadError
import cats.data.{EitherT, ReaderT}
import cats.effect.{Sync, Timer}
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import org.log4s.Logger
import doobie.implicits._

import scala.concurrent.duration.FiniteDuration

package object infrastructure {

  object implicits {
    private def measureAny[F[_], T, R](ret: Long => F[Unit], f: T => F[R])(implicit T: Timer[F], M: Sync[F]): (T) => F[R] = {
      in =>
        for {
          start <- T.clock.monotonic(TimeUnit.NANOSECONDS)
          run <- f(in)
          end <- T.clock.monotonic(TimeUnit.NANOSECONDS)
          _ <- ret(end - start)
        } yield run
    }

    private def monkey[F[_], R](failureRate: Double)(result: F[R])(implicit E: MonadError[F, Throwable], S: Sync[F]): F[R] = {
      for {
        fail <- S.delay(scala.util.Random.nextDouble() <= failureRate)
        r <- if (fail) {
          E.raiseError[R](new RuntimeException("Chaos monkey striked!"))
        } else {
          result
        }
      } yield r
    }

    private def ret[F[_], R](times: Int, delay: FiniteDuration)(result: F[R])(implicit E: MonadError[F, Throwable], T: Timer[F]): F[R] = {
      if (times <= 0) {
        result
      } else {
        result.handleErrorWith { e =>
          T.sleep(delay) *> ret(times - 1, delay)(result)
        }
      }
    }

    implicit final class EffectFunction1Ops[F[_], T1, T2](val f: Function1[T1, F[T2]]) extends AnyVal {
      def measure(ret: Long => F[Unit])(implicit T: Timer[F], M: Sync[F]): T1 => F[T2] = {
        measureAny(ret, f).apply
      }
    }

    implicit final class EffectFunction2Ops[F[_], T1, T2, T3](val f: Function2[T1, T2, F[T3]]) extends AnyVal {
      def logErrors(logger: Logger)(implicit E: MonadError[F, Throwable], S: Sync[F]): (T1, T2) => F[T3] = {
        f.andThen(_.onError {
          case e => S.delay(logger.error(e)("Got an unrecoverable error!"))
        })
      }

      def measure(ret: Long => F[Unit])(implicit T: Timer[F], M: Sync[F]): (T1, T2) => F[T3] = {
        (p1, p2) => measureAny(ret, f.tupled).apply((p1, p2))
      }

      def retry(times: Int, delay: FiniteDuration)(implicit E: MonadError[F, Throwable], T: Timer[F]): (T1, T2) => F[T3] = {
        f.andThen(ret(times, delay))
      }

      def chaosMonkey(failureRate: Double)(implicit E: MonadError[F, Throwable], S: Sync[F]): (T1, T2) => F[T3] = {
        (p1, p2) => monkey[F, T3](failureRate)(f(p1, p2))
      }
    }

    implicit final class Function2Ops[T1, T2, T3](private val f: Function2[T1, T2, T3]) extends AnyVal {
      def andThen[A](g: T3 => A): (T1, T2) => A = { (x, y) => g(f.apply(x, y)) }
    }

    implicit final class ConIOResultReaderOps[C, E, R](private val result: Result[ReaderT[ConnectionIO, C, *], E, R]) extends AnyVal {
      def transact[G[_] : Sync](xa: Transactor[G]): Result[ReaderT[G, C, *], E, R] = {
        result
          .leftSemiflatMap(err => ReaderT.liftF(doobie.hi.HC.rollback.as(err)))
          .value
          .mapK(xa.trans)
          .asResult
      }
    }

    implicit final class ResultReaderOps[F[_], C, E, R](private val result: Result[ReaderT[F, C, *], E, R]) extends AnyVal {
      def local[C1](c: C1 => C): Result[ReaderT[F, C1, *], E, R] = {
        result.value.local[C1](c).asResult
      }

      def run(tr: C): Result[F, E, R] = {
        result.value.run(tr).asResult
      }
    }

    implicit final class ResultOps[F[_], E, R](private val result: Result[F, E, R]) extends AnyVal {
      def liftReader[C]: Result[ReaderT[F, C, *], E, R] = {
        ReaderT.liftF[F, C, Either[E, R]](result.value).asResult
      }
    }

    implicit final class EffectOps[F[_], R](private val result: F[R]) extends AnyVal {
      def liftReader[C]: ReaderT[F, C, R] = ReaderT.liftF[F, C, R](result)
    }

  }

}
