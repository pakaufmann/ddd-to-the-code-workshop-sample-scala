package com.github.pkaufmann.dddttc.accounting.infrastructure.persistence

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb[F[_] : Async](driverClass: String, url: String, user: String, password: String)(implicit context: ContextShift[F]): Resource[F, Transactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) // our connect EC
      be <- Blocker[F] // our blocking EC
      xa <- HikariTransactor
        .newHikariTransactor[F](
          driverClass,
          url,
          user,
          password,
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
