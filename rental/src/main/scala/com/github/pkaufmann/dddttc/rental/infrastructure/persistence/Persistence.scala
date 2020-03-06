package com.github.pkaufmann.dddttc.rental.infrastructure.persistence

import cats.Monad
import cats.effect.{Blocker, ContextShift, IO, Resource}
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import com.github.pkaufmann.dddttc.rental.application.BikeService
import com.github.pkaufmann.dddttc.rental.application.domain.bike.NumberPlate
import doobie._
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Persistence {
  def initDb()(implicit context: ContextShift[IO]): Resource[IO, IOTransaction[ConnectionIO]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO] // our blocking EC
      xa <- HikariTransactor
        .newHikariTransactor[IO](
          "org.h2.Driver",
          "jdbc:h2:mem:rental;DB_CLOSE_DELAY=-1",
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

  def initializeDb[F[_] : Monad](bikeService: BikeService[F]): F[Unit] = {
    for {
      _ <- bikeService.addBike(NumberPlate("1"))
      _ <- bikeService.addBike(NumberPlate("2"))
      _ <- bikeService.addBike(NumberPlate("3"))
    } yield ()
  }
}
