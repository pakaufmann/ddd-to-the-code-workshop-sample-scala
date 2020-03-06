package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.effect.{Resource, Sync}
import cats.implicits._
import cats.tagless.Derive
import javax.jms.{MessageProducer, Session}
import org.log4s._

trait EventPublisher[F[_]] {
  def publish(): F[Unit]
}

object EventPublisher {
  def fromStore[F[_] : Sync](eventStore: EventStore[F], count: Int = 20): EventPublisher[ReaderT[F, Session, *]] =
    new PendingEventPublisher[F](eventStore, count)

  class PendingEventPublisher[F[_]](eventStore: EventStore[F], count: Int = 20)(implicit S: Sync[F]) extends EventPublisher[ReaderT[F, Session, *]] {
    private val logger = getLogger

    override def publish(): ReaderT[F, Session, Unit] = {
      cats.data.ReaderT[F, Session, Unit] { session =>
        val sent = for {
          unsent <- eventStore.loadUnsent(count)
          _ <- unsent.traverse(e => sendEvent(session, e))
          _ <- eventStore.removeSent(unsent)
        } yield ()

        sent.handleErrorWith(e => Sync[F].delay(logger.error(e)("Error while trying to send events")))
      }
    }

    private def sendEvent(session: Session, event: PendingDomainEvent): F[Unit] = {
      val producer = S.delay {
        session.createProducer(session.createTopic(event.topic.name))
      }

      def closeProducer(s: MessageProducer) = S.delay {
        s.close()
      }

      Resource.make(producer)(closeProducer).use { p =>
        S.delay {
          val message = session.createTextMessage()
          message.setStringProperty("domain-event-id", event.id)
          message.setText(event.payload)
          message.setJMSCorrelationID(event.trace.id)
          p.send(message)
          logger.info(s"Sent event: $event")
        }
      }
    }
  }

}