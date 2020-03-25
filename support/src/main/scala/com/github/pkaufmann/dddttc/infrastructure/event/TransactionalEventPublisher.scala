package com.github.pkaufmann.dddttc.infrastructure.event

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.data.ReaderT
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.domain.Publisher
import com.github.pkaufmann.dddttc.infrastructure.Trace
import doobie.implicits.javatime._
import shapeless._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound("Could not find a publication for message type ${T}")
trait MqPublication[T] {
  def topic: Topic

  def asPayload(in: T): String
}

object MqPublication {
  def create[T](c: Topic, encoder: T => String): MqPublication[T] = new MqPublication[T] {
    override val topic: Topic = c

    override def asPayload(in: T): String = encoder(in)
  }

  def apply[T](implicit ev: MqPublication[T]) = ev
}

object TransactionalEventPublisher {
  def single[F[_] : Sync : Clock, T: MqPublication](storeEvent: EventStore.Store[F]): Publisher[F, T] = {
    singleTracing(storeEvent).andThen(_.run(Trace()))
  }

  def singleTracing[F[_] : Sync : Clock, T](storeEvent: EventStore.Store[F])(implicit mapper: MqPublication[T]): Publisher[ReaderT[F, Trace, *], T] =
    event => ReaderT { trace =>
      publish(mapper.topic, mapper.asPayload(event), trace, storeEvent)
    }

  def multi[F[_] : Sync : Clock, T <: Coproduct]
  (
    storeEvent: EventStore.Store[F]
  )
  (
    implicit @implicitNotFound("Could not find publication for all given messages in ${T}") ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]
  ): Publisher[F, T] = {
    multiTracing(storeEvent).andThen(_.run(Trace()))
  }

  def multiTracing[F[_] : Sync : Clock, T <: Coproduct]
  (
    storeEvent: EventStore.Store[F]
  )(
    implicit @implicitNotFound("Could not find publication for all given messages in ${T}") ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]
  ): Publisher[ReaderT[F, Trace, *], T] = {
    {
      event =>
        ReaderT { trace =>
          val (topic, payload) = ev(event)
          publish(topic, payload, trace, storeEvent)
        }
    }
  }

  private def publish[F[_], T](topic: Topic, payload: String, trace: Trace, storeEvent: EventStore.Store[F])(implicit C: Clock[F], S: Sync[F]): F[Unit] = {
    for {
      id <- S.delay(UUID.randomUUID().toString)
      time <- C.realTime(TimeUnit.MILLISECONDS)
      publishedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC)
      _ <- storeEvent(PendingDomainEvent(id, topic, payload, publishedAt, trace))
    } yield ()
  }
}

object EventPublisherMapper extends Poly1 {
  implicit def event[A](implicit pub: MqPublication[A]) = at((e: A) => (pub.topic, pub.asPayload(e)))
}