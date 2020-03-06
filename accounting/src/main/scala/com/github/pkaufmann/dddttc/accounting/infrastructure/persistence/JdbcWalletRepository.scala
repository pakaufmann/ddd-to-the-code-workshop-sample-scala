package com.github.pkaufmann.dddttc.accounting.infrastructure.persistence

import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import doobie._
import doobie.implicits._

class JdbcWalletRepository extends WalletRepository[ConnectionIO] {
  override def findAll(): ConnectionIO[List[Wallet]] = {
    sql"SELECT data FROM wallet".query[Wallet]
      .stream
      .compile
      .toList
  }

  override def get(userId: UserId): Result[ConnectionIO, WalletNotExistingError, Wallet] = {
    sql"SELECT data FROM wallet WHERE id = $userId"
      .query[Wallet]
      .option
      .map(_.toRight(WalletNotExistingError(userId)))
      .asResult
  }

  override def add(wallet: Wallet): Result[ConnectionIO, WalletAlreadyExistsError, Unit] = {
    sql"INSERT INTO wallet(id, data) VALUES (${wallet.id}, $wallet)"
      .update
      .run
      .attemptSomeSqlState {
        case SqlState(value) if value.contains("23505") =>
          WalletAlreadyExistsError(wallet.id)
      }
      .asResult
      .map(_ => ())
  }

  override def update(wallet: Wallet): Result[ConnectionIO, WalletNotExistingError, Unit] = {
    sql"UPDATE wallet SET data = $wallet WHERE id = ${wallet.id}"
      .update
      .run
      .map {
        case 0 => Left(WalletNotExistingError(wallet.id))
        case _ => Right()
      }
      .asResult
  }
}
