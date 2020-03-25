package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.domain.Result

object WalletRepository {
  type FindAll[F[_]] = F[List[Wallet]]

  type Get[F[_]] = UserId => Result[F, WalletNotExistingError, Wallet]

  type Add[F[_]] = Wallet => Result[F, WalletAlreadyExistsError, Unit]

  type Update[F[_]] = Wallet => Result[F, WalletNotExistingError, Unit]
}
