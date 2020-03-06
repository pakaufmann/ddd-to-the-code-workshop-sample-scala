package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.effect.{Resource, Sync, Timer}
import cats.implicits._
import javax.jms.{ConnectionFactory, Session}

import scala.concurrent.duration.{FiniteDuration, _}

object MqSession {
  def withSession[F[_] : Timer](connFactory: ConnectionFactory, every: FiniteDuration = 1.second)(runWithSession: ReaderT[F, Session, Unit])(implicit S: Sync[F]): F[Unit] = {
    val session = for {
      connection <- Resource.make(S.delay(connFactory.createConnection()))(c => S.delay(c.close()).handleErrorWith(_ => S.unit))
      session <- Resource.make(S.delay(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)))(s => S.delay(s.close()).handleErrorWith(_ => S.unit))
    } yield session

    fs2.Stream.resource(session)
      .evalMap(runWithSession.run)
      .delayBy(every)
      .repeat
      .compile
      .drain
  }
}
