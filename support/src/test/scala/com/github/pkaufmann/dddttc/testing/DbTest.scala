package com.github.pkaufmann.dddttc.testing

import cats.effect._
import com.github.pkaufmann.dddttc.domain.Result
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.{ConnectionIO, ExecutionContexts, HC, Transactor}
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

trait DbTest {
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private val transactor: Resource[IO, Transactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
    be <- Blocker[IO] // our blocking EC
    xa <- H2Transactor.newH2Transactor[IO](
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      "sa",
      "",
      ce,
      be)
      .evalTap(e => e.configure(d => IO(Flyway.configure().dataSource(d).load().migrate())))
    xa2 = Transactor.after.set(xa, HC.rollback)
  } yield xa2

  def run[E, T](in: Result[ConnectionIO, E, T]): Either[E, T] = {
    run(in.value)
  }

  def run[T](in: ConnectionIO[T]): T = {
    transactor.use { xa =>
      in.transact(xa)
    }.unsafeRunSync()
  }
}