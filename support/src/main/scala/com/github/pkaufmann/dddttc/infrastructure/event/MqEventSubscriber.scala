package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.effect.implicits._
import cats.effect.{Blocker, ContextShift, ExitCode, Resource, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.infrastructure.Trace

import javax.jms.{ConnectionFactory, Session, TextMessage}
import org.log4s._

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Could not find a subscription for message type ${T}")
trait MqSubscription[T] {
  def topic: Topic

  def asObject(in: String): Try[T]
}

object MqSubscription {
  def create[T](c: Topic, decoder: String => Try[T]): MqSubscription[T] = new MqSubscription[T] {
    override val topic: Topic = c

    override def asObject(in: String): Try[T] = decoder(in)
  }

  def apply[T](implicit ev: MqSubscription[T]) = ev
}

object MqEventSubscriber {
  type MessageSubscription[F[_], T] = (T, TextMessage) => F[Unit]

  private val logger = getLogger

  def bindTrace[F[_] : Sync, T: MqSubscription](connFactory: ConnectionFactory, handle: Subscription[ReaderT[F, Trace, *], T])(implicit cs: ContextShift[F]): F[ExitCode] = {
    bindMessage[F, T](connFactory) {
      (m, t) => handle.andThen(_.run(Trace(t.getJMSCorrelationID))).apply(m)
    }
  }

  def bind[F[_] : Sync, T: MqSubscription](connFactory: ConnectionFactory, handle: Subscription[F, T])(implicit cs: ContextShift[F]): F[ExitCode] = {
    bindMessage[F, T](connFactory) {
      (m, _) => handle(m)
    }
  }

  private def bindMessage[F[_] : Sync, T](connFactory: ConnectionFactory)(handle: MessageSubscription[F, T])(implicit subscription: MqSubscription[T], cs: ContextShift[F]): F[ExitCode] = {
    fs2.Stream.resource(createConsumer[F, T](connFactory))
      .evalMap { in =>
        Blocker[F].use { blocker =>
          for {
            message <- blocker.delay(in.receive())
            _ <- message match {
              case message: TextMessage =>
                subscription.asObject(message.getText)
                  .fold(
                    Sync[F].raiseError[Unit],
                    event => for {
                      _ <- Sync[F].delay[Unit](logger.info(s"Received event: $event with trace: ${message.getJMSCorrelationID}"))
                      r <- handle(event, message).map(_ => ())
                    } yield r
                  )
                  .onError(e => Sync[F].delay(logger.error(e)("An error occurred while trying to consume the events")))
                  .guarantee(Sync[F].delay(message.acknowledge()))
              case _ =>
                Sync[F].unit
            }
          } yield ()
        }
      }
      .repeat
      .compile
      .drain
      .as(ExitCode.Error)
  }

  private def createConsumer[F[_] : Sync, T](connFactory: ConnectionFactory)(implicit s: MqSubscription[T]) = {
    for {
      connection <- Resource.make(Sync[F].delay({
        val conn = connFactory.createConnection()
        conn.start()
        conn
      }))(c => Sync[F].delay(c.close()).handleErrorWith(_ => Sync[F].unit))
      session <- Resource.make(Sync[F].delay(connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)))(s => Sync[F].delay(s.close()).handleErrorWith(_ => Sync[F].unit))
      consumer <- Resource.make(Sync[F].delay(session.createConsumer(session.createTopic(s.topic.name))))(c => Sync[F].delay(c.close()).handleErrorWith(_ => Sync[F].unit))
    } yield consumer
  }
}