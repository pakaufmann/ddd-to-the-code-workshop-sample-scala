package com.github.pkaufmann.dddttc.infrastructure.event

import cats.effect._
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import doobie._
import javax.jms.{ConnectionFactory, MessageProducer, Session}
import org.log4s._

import scala.concurrent.duration._

class PendingEventPublisher(eventStore: PendingEventStore, connFactory: ConnectionFactory)(implicit xa: IOTransaction[ConnectionIO]) {
  private val logger = getLogger

  def start(count: Int = 20, every: FiniteDuration = 1.second)(implicit cs: ContextShift[IO], timer: Timer[IO]): IO[Fiber[IO, Unit]] = {
    createSession(connFactory).use { session =>
      fs2.Stream.awakeDelay[IO](every)
        .evalMap(_ => {
          publish(session, count)
            .transact
            .handleErrorWith(e => IO(logger.error(e)("Error when trying to send events")))
        })
        .compile
        .drain
    }.start
  }

  private def createSession(connFactory: ConnectionFactory) = {
    for {
      connection <- Resource.make(IO(connFactory.createConnection()))(c => IO(c.close()).handleErrorWith(_ => IO.pure()))
      session <- Resource.make(IO(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)))(s => IO(s.close()).handleErrorWith(_ => IO.pure()))
    } yield session
  }

  private def publish(session: Session, count: Int) = {
    for {
      unsent <- eventStore.loadUnsent(count)
      _ <- Async[ConnectionIO].liftIO(unsent.traverse(e => sendEvent(session, e)))
      _ <- eventStore.removeSent(unsent)
    } yield ()
  }

  private def sendEvent(session: Session, event: PendingDomainEvent): IO[Unit] = {
    val producer = IO {
      session.createProducer(session.createTopic(event.topic.name))
    }

    def closeProducer(s: MessageProducer) = IO {
      s.close()
    }

    Resource.make(producer)(closeProducer).use { p =>
      IO {
        val message = session.createTextMessage()
        message.setStringProperty("domain-event-id", event.id)
        message.setText(event.payload)
        p.send(message)
        logger.info(s"Sent event: $event")
      }
    }
  }
}