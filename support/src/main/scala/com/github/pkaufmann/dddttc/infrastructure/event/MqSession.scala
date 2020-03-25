package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.implicits._
import cats.effect.{Resource, Sync, Timer}
import javax.jms.{ConnectionFactory, Session}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object MqSession {
  def withSession[F[_] : Sync : Timer](connFactory: ConnectionFactory, every: FiniteDuration = 1.second)(repeat: ReaderT[F, Session, Unit]): F[Unit] = {
    fs2.Stream.resource(createSession(connFactory))
      .evalMap(repeat.run)
      .delayBy(every)
      .repeat
      .compile
      .drain
  }

  def createSession[F[_]](connFactory: ConnectionFactory)(implicit sync: Sync[F]) = {
    for {
      connection <- Resource.make(sync.delay(connFactory.createConnection()))(c => sync.delay(c.close()).handleErrorWith(_ => sync.unit))
      session <- Resource.make(sync.delay(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)))(s => sync.delay(s.close()).handleErrorWith(_ => sync.unit))
    } yield session
  }
}
