package com.github.pkaufmann.dddttc.accounting.infrastructure.web

import cats.effect.Sync
import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import org.http4s.dsl.io._
import org.http4s.twirl._
import org.http4s.{HttpRoutes, Response, Status}

object AccountingController {
  def listWallets[F[_] : Sync](listWallets: WalletService.ListWallets[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "wallets" =>
        listWallets
          .map(wallets => Response(status = Status.Ok).withEntity(accounting.html.wallets(wallets)))
    }
  }
}
