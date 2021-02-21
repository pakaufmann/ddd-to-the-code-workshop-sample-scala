package com.github.pkaufmann.dddttc.infrastructure.event

import cats.data.ReaderT
import cats.effect.{Resource, Sync}
import cats.implicits._
import javax.jms.Session
import org.log4s.getLogger

object MqEventPublisher {

  private val logger = getLogger

  def publish[F[_] : Sync]
  (
    getUnsent: EventStore.GetUnsent[F], removeSent: EventStore.RemoveSent[F],
    count: Int = 20
  ): ReaderT[F, Session, Unit] =
    ReaderT[F, Session, Unit] { session =>
      val sent = for {
        unsent <- getUnsent(count)
        _ <- unsent.traverse(e => sendEvent[F](session, e))
        _ <- removeSent(unsent)
      } yield ()

      sent.handleErrorWith(e => Sync[F].delay(logger.error(e)("Error while trying to send events")))
    }

  private def sendEvent[F[_]](session: Session, event: PendingDomainEvent)(implicit sync: Sync[F]): F[Unit] = {
    val producer = sync.delay {
      session.createProducer(session.createTopic(event.topic.name))
    }

    Resource.make(producer)(s => sync.delay(s.close()).handleErrorWith(_ => sync.unit)).use { p =>
      sync.delay {
        val message = session.createTextMessage()
        message.setStringProperty("domain_event_id", event.id)
        message.setText(event.payload)
        message.setJMSCorrelationID(event.trace.id)
        p.send(message)
        logger.info(s"Sent event: $event")
      }
    }
  }
}
