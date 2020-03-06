package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import cats.Monad
import cats.effect.{Async, Blocker, ContextShift, IO, Resource, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.rental.application.BikeService
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb[F[_]]()(implicit context: ContextShift[F], S: Async[F]): Resource[F, Transactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) // our connect EC
      be <- Blocker[F] // our blocking EC
      xa <- HikariTransactor
        .newHikariTransactor[F](
          "org.h2.Driver",
          "jdbc:h2:mem:rental;DB_CLOSE_DELAY=-1",
          "sa",
          "",
          ce,
          be
        )
        .evalTap(_.configure(s => S.delay(s.setAutoCommit(false))))
        .evalTap(runMigrations[F])
    } yield xa
  }


  private def runMigrations[F[_]](transactor: HikariTransactor[F])(implicit S: Sync[F]): F[Unit] = {
    transactor.configure { dataSource =>
      S.delay {
        Flyway.configure().dataSource(dataSource).load().migrate()
      }
    }
  }

  def initializeDb[F[_] : Monad](bikeService: BikeService[F]): F[Unit] = {
    for {
      _ <- bikeService.addBike(NumberPlate("1"))
      _ <- bikeService.addBike(NumberPlate("2"))
      _ <- bikeService.addBike(NumberPlate("3"))
    } yield ()
  }
}
