package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import cats.Monad
import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.rental.application.BikeService
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
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

  def initializeDb[F[_] : Monad](addBike: BikeService.AddBike[F]): F[Unit] = {
    for {
      _ <- addBike(NumberPlate("1"))
      _ <- addBike(NumberPlate("2"))
      _ <- addBike(NumberPlate("3"))
    } yield ()
  }
}
