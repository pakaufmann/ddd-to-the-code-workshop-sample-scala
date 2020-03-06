package com.github.pkaufmann.dddttc.infrastructure.event

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import cats.data.ReaderT
import com.github.pkaufmann.dddttc.domain.Publisher
import com.github.pkaufmann.dddttc.infrastructure.Trace
import doobie.implicits.javatime._
import shapeless._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound("Could not find a publication for message type ${T}")
trait MqPublication[T] {
  def topic(): Topic

  def asPayload(in: T): String
}

object MqPublication {
  def apply[T](c: Topic, encoder: T => String): MqPublication[T] = new MqPublication[T] {
    override def topic(): Topic = c

    override def asPayload(in: T): String = encoder(in)
  }
}

object EventPublisherMapper extends Poly1 {
  implicit def event[A](implicit pub: MqPublication[A]) = at((e: A) => (pub.topic(), pub.asPayload(e)))
}

object TransactionalEventPublisher {
  def single[F[_], T: MqPublication](clock: Clock, eventStore: EventStore[F]): Publisher[F, T] =
    event => singleTracing(clock, eventStore).apply(event).run(Trace())

  def multi[F[_], T <: Coproduct]
  (
    clock: Clock, eventStore: EventStore[F])
  (
    implicit @implicitNotFound("Could not find publication for all given messages in ${T}") ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]
  ): Publisher[F, T] =
    event => multiTracing(clock, eventStore).apply(event).run(Trace())

  def singleTracing[F[_], T: MqPublication](clock: Clock, eventStore: EventStore[F]): Publisher[ReaderT[F, Trace, *], T] =
    event => ReaderT { trace: Trace =>
      val id = UUID.randomUUID().toString
      val publishedAt = LocalDateTime.now(clock)
      val mapper = implicitly[MqPublication[T]]
      eventStore.store(PendingDomainEvent(id, mapper.topic(), mapper.asPayload(event), publishedAt, trace))
    }

  def multiTracing[F[_], T <: Coproduct]
  (
    clock: Clock, eventStore: EventStore[F]
  )(
    implicit @implicitNotFound("Could not find publication for all given messages in ${T}") ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]
  ): Publisher[ReaderT[F, Trace, *], T] =
    event => ReaderT { trace =>
      val id = UUID.randomUUID().toString
      val publishedAt = LocalDateTime.now(clock)
      val (topic, payload) = ev(event)
      eventStore.store(PendingDomainEvent(id, topic, payload, publishedAt, trace))
    }
}