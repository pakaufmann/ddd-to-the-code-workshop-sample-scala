package com.github.pkaufmann.dddttc.accounting.infrastructure.web

import cats.data.ReaderT
import cats.effect.{IO, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.infrastructure.Trace
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.io._
import org.http4s.twirl._
import doobie.implicits._

object AccountingController {
  def routes[F[_] : Sync](walletService: WalletService[ReaderT[ConnectionIO, Trace, *]])(implicit xa: Transactor[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "wallets" =>
        walletService
          .listWallets()
          .run(Trace())
          .transact(xa)
          .map { wallets =>
            Response(status = Status.Ok)
              .withEntity(accounting.html.wallets(wallets))
          }
    }
  }
}
