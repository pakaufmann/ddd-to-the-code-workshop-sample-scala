package com.github.pkaufmann.dddttc.accounting.infrastructure.web

import cats.effect.IO
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.twirl._

object AccountingController {
  def routes[F[_] : IOTransaction](walletService: WalletService[F]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "wallets" =>
        walletService
          .listWallets()
          .transact
          .flatMap(wallets => Ok(accounting.html.wallets(wallets)))
    }
  }
}
