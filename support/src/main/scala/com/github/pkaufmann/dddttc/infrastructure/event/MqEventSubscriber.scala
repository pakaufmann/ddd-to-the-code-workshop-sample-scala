package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.effect.implicits._
import cats.effect.{ExitCode, Resource, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Subscription
import com.github.pkaufmann.dddttc.infrastructure.Trace
import javax.jms.{ConnectionFactory, Session, TextMessage}
import org.log4s._

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Could not find a subscription for message type ${T}")
trait MqSubscription[T] {
  def topic(): Topic

  def asObject(in: String): Try[T]
}

object MqSubscription {
  def create[T](c: Topic, decoder: String => Try[T]): MqSubscription[T] = new MqSubscription[T] {
    override def topic(): Topic = c

    override def asObject(in: String): Try[T] = decoder(in)
  }

  def apply[T](implicit ev: MqSubscription[T]) = ev
}

class MqEventSubscriber(connFactory: ConnectionFactory) {
  type MessageSubscription[F[_], T] = (T, TextMessage) => F[Unit]

  private val logger = getLogger

  def bindTrace[F[_] : Sync, T: MqSubscription](handle: Subscription[ReaderT[F, Trace, *], T]): F[ExitCode] = {
    bindSubscription[F, T] {
      (m, t) => handle.andThen(_.run(Trace(t.getJMSCorrelationID))).apply(m)
    }
  }

  def bind[F[_] : Sync, T: MqSubscription](handle: Subscription[F, T]): F[ExitCode] = {
    bindSubscription[F, T] {
      (m, _) => handle(m)
    }
  }

  private def bindSubscription[F[_] : Sync, T](handle: MessageSubscription[F, T])(implicit subscription: MqSubscription[T]): F[ExitCode] = {
    fs2.Stream.resource(createConsumer[F, T])
      .evalMap {
        _.receive() match {
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
      }
      .repeat
      .compile
      .drain
      .as(ExitCode.Error)
  }

  private def createConsumer[F[_] : Sync, T](implicit s: MqSubscription[T]) = {
    for {
      connection <- Resource.make(Sync[F].delay({
        val conn = connFactory.createConnection()
        conn.start()
        conn
      }))(c => Sync[F].delay(c.close()).handleErrorWith(_ => Sync[F].unit))
      session <- Resource.make(Sync[F].delay(connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)))(s => Sync[F].delay(s.close()).handleErrorWith(_ => Sync[F].unit))
      consumer <- Resource.make(Sync[F].delay(session.createConsumer(session.createTopic(s.topic().name))))(c => Sync[F].delay(c.close()).handleErrorWith(_ => Sync[F].unit))
    } yield consumer
  }
}