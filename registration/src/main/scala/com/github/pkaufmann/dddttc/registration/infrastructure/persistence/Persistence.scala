package com.github.pkaufmann.dddttc.registration.infrastructure.persistence

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb[F[_]]()(implicit context: ContextShift[F], S: Async[F]): Resource[F, Transactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      be <- Blocker[F]
      xa <- HikariTransactor
        .newHikariTransactor[F](
          "org.h2.Driver",
          "jdbc:h2:mem:registration;DB_CLOSE_DELAY=-1",
          "sa",
          "",
          ce,
          be
        )
        .evalTap(_.configure(s => S.delay(s.setAutoCommit(false))))
        .evalTap(a => runMigrations(a))
    } yield xa
  }


  private def runMigrations[F[_]](transactor: HikariTransactor[F])(implicit S: Sync[F]): F[Unit] = {
    transactor.configure { dataSource =>
      S.delay {
        Flyway.configure().dataSource(dataSource).load().migrate()
      }
    }
  }
}
