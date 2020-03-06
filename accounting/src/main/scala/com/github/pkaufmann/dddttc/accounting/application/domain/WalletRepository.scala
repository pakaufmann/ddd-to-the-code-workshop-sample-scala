package com.github.pkaufmann.dddttc.accounting.application.domain

import com.github.pkaufmann.dddttc.domain.Result

trait WalletRepository[F[_]] {
  def findAll(): F[List[Wallet]]

  def get(userId: UserId): Result[F, WalletNotExistingError, Wallet]

  def add(newWallet: Wallet): Result[F, WalletAlreadyExistsError, Unit]

  def update(billedWallet: Wallet): Result[F, WalletNotExistingError, Unit]
}
