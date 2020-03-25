package com.github.pkaufmann.dddttc.accounting.infrastructure.persistence

import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.domain.Result
import com.github.pkaufmann.dddttc.domain.implicits._
import doobie._
import doobie.implicits._

object JdbcWalletRepository {
  val findAll: WalletRepository.FindAll[ConnectionIO] = {
    sql"SELECT data FROM wallet".query[Wallet]
      .stream
      .compile
      .toList
  }

  val get: WalletRepository.Get[ConnectionIO] = {
    userId => {
      sql"SELECT data FROM wallet WHERE id = $userId"
        .query[Wallet]
        .option
        .map(_.toRight(WalletNotExistingError(userId)))
        .asResult
    }
  }

  val add: WalletRepository.Add[ConnectionIO] = {
    wallet => {
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
  }

  val update: WalletRepository.Update[ConnectionIO] = {
    wallet => {
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
}
