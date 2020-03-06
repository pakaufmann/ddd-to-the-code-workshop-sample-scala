package com.github.pkaufmann.dddttc.accounting.infrastructure

import java.time.{Clock, Instant, ZoneOffset}

import cats.implicits._
import com.github.pkaufmann.dddttc.accounting.application.WalletService
import com.github.pkaufmann.dddttc.accounting.application.domain._
import com.github.pkaufmann.dddttc.accounting.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.accounting.infrastructure.persistence.JdbcWalletRepository
import com.github.pkaufmann.dddttc.infrastructure.event.{PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.testing.AggregateBuilder._
import com.github.pkaufmann.dddttc.testing.DbTest
import doobie.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import shapeless.record._
import TestWallets._

class WalletScenarioTest extends AnyFlatSpec with Matchers with AggregateMatchers with DbTest {
  val clock = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC)

  val walletService = WalletService(
    new BookingFeePolicy(),
    JdbcWalletRepository,
    TransactionalEventPublisher.single(clock, PendingEventStore)
  )

  "The wallet service" should "create a new wallet" in {
    val wallet = run {
      for {
        _ <- walletService.initializeWallet(UserId("1"))
        w <- JdbcWalletRepository.get(UserId("1")).leftWiden[Any]
      } yield w
    }

    wallet.getOrElse(fail()) should eqv(TestWallets.change
      .replace(Symbol("id"), UserId("1"))
      .back[Wallet]
    )
  }

  it should "return an error when a wallet already exists" in {
    val result = run {
      for {
        _ <- JdbcWalletRepository.add(TestWallets.default)
        r <- walletService.initializeWallet(UserId("1"))
      } yield r
    }

    result shouldBe Left(WalletAlreadyExistsError(UserId("1")))
  }

  it should "list all existing wallets" in {
    val wallets = List(
      TestWallets.default,
      TestWallets.change.replace(Symbol("id"), UserId("2")).back[Wallet]
    )

    val foundWallets = run {
      for {
        _ <- wallets.traverse(JdbcWalletRepository.add).value
        found <- walletService.listWallets()
      } yield found
    }

    foundWallets should have size 2
    foundWallets should contain(TestWallets.default)
    foundWallets should contain(TestWallets.change.replace(Symbol("id"), UserId("2")).back[Wallet])
  }
}
