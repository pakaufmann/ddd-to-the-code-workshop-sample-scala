package com.github.pkaufmann.dddttc.registration.infrastructure.persistence

import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb()(implicit context: ContextShift[IO]): Resource[IO, IOTransaction[ConnectionIO]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      be <- Blocker[IO]
      xa <- HikariTransactor
        .newHikariTransactor[IO](
          "org.h2.Driver",
          "jdbc:h2:mem:registration;DB_CLOSE_DELAY=-1",
          "sa",
          "",
          ce,
          be
        )
        .evalTap(_.configure(s => IO(s.setAutoCommit(false))))
        .evalTap(runMigrations)
    } yield JdbcTransaction(xa)
  }


  private def runMigrations(transactor: HikariTransactor[IO]): IO[Unit] = {
    transactor.configure { dataSource =>
      IO {
        Flyway.configure().dataSource(dataSource).load().migrate()
      }
    }
  }
}
