package com.github.pkaufmann.dddttc.infrastructure.event

import cats.effect.{ContextShift, ExitCode, Fiber, IO, Resource}
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.events.Subscription
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
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
  def createSubscription[F[_] : IOTransaction, T: MqSubscription](): Subscription[F, T] = {
    (m: T => F[_]) => subscribe(m)
  }

  private val logger = getLogger

  private var handles: List[IO[Unit]] = List.empty[IO[Unit]]

  def subscribe[F[_] : IOTransaction, T](messageHandler: T => F[_])(implicit subscription: MqSubscription[T]): Unit = {
    handles = createConsumer[T](connFactory).use { consumer =>
      fs2.Stream.eval(IO(consumer.receive()))
        .repeat
        .evalMap({
          case message: TextMessage =>
            subscription.asObject(message.getText)
              .fold(
                IO.raiseError,
                m => for {
                  _ <- IO(logger.info(s"Received event: $m"))
                  r <- messageHandler(m).transact
                } yield r
              )
              .onError(e => IO(logger.error(e)("An error occurred while trying to consume the events")))
              .guarantee(IO(message.acknowledge()))
          case _ =>
            IO.unit
        })
        .compile
        .drain
    } +: handles
  }

  def start()(implicit cs: ContextShift[IO]): IO[List[Fiber[IO, ExitCode]]] =
    handles.traverse(_.start.map(_.map(_ => ExitCode.Error)))

  private def createConsumer[T](connFactory: ConnectionFactory)(implicit s: MqSubscription[T]) = {
    for {
      connection <- Resource.make(IO({
        val conn = connFactory.createConnection()
        conn.start()
        conn
      }))(c => IO(c.close()).handleErrorWith(_ => IO.pure()))
      session <- Resource.make(IO(connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)))(s => IO(s.close()).handleErrorWith(_ => IO.pure()))
      consumer <- Resource.make(IO(session.createConsumer(session.createTopic(s.topic().name))))(c => IO(c.close()).handleErrorWith(_ => IO.pure()))
    } yield consumer
  }
}