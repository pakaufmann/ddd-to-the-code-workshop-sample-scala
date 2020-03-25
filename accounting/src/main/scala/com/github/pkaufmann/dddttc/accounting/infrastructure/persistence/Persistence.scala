package com.github.pkaufmann.dddttc.accounting.infrastructure.persistence

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb[F[_] : Async]()(implicit context: ContextShift[F]): Resource[F, Transactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      be <- Blocker[F]
      xa <- HikariTransactor
        .newHikariTransactor[F](
          "org.h2.Driver",
          "jdbc:h2:mem:accounting;DB_CLOSE_DELAY=-1",
          "sa",
          "",
          ce,
          be
        )
        .evalTap(_.configure(s => Sync[F].delay(s.setAutoCommit(false))))
        .evalTap(runMigrations[F])
    } yield xa
  }


  private def runMigrations[F[_] : Sync](transactor: HikariTransactor[F]): F[Unit] = {
    transactor.configure { dataSource =>
      Sync[F].delay {
        Flyway.configure().dataSource(dataSource).load().migrate()
      }
    }
  }
}
