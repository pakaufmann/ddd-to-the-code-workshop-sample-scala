package com.github.pkaufmann.dddttc.accounting.infrastructure.persistence

import cats.effect.IO
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits.IOTransaction
import doobie._
import doobie.free.connection.ConnectionIO
import doobie.implicits._

case class JdbcTransaction(xa: Transactor[IO]) extends IOTransaction[ConnectionIO] {
  override def transact[R](in: ConnectionIO[R]): IO[R] = {
    in.transact(xa)
  }

  override def transact[E, R](in: Result[ConnectionIO, E, R]): Result[IO, E, R] = in.transact(xa)
}
