package com.github.pkaufmann.dddttc.infrastructure.event

import java.time.{Clock, LocalDateTime}
import java.util.UUID

import com.github.pkaufmann.dddttc.domain.events.Publisher
import doobie._
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

class MultiTransactionalEventPublisher[T <: Coproduct](clock: Clock, eventStore: PendingEventStore)(implicit ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]) extends Publisher[ConnectionIO, T] {
  override def publish(event: T): ConnectionIO[Unit] = {
    val id = UUID.randomUUID().toString
    val publishedAt = LocalDateTime.now(clock)
    val (topic, payload) = ev(event)
    eventStore.store(PendingDomainEvent(id, topic, payload, publishedAt))
  }
}

object EventPublisherMapper extends Poly1 {
  implicit def event[A](implicit pub: MqPublication[A]) = at((e: A) => (pub.topic(), pub.asPayload(e)))
}

class SingleTransactionalEventPublisher[T: MqPublication](clock: Clock, eventStore: PendingEventStore) extends Publisher[ConnectionIO, T] {
  override def publish(event: T): ConnectionIO[Unit] = {
    val id = UUID.randomUUID().toString
    val publishedAt = LocalDateTime.now(clock)
    val mapper = implicitly[MqPublication[T]]
    eventStore.store(PendingDomainEvent(id, mapper.topic(), mapper.asPayload(event), publishedAt))
  }
}

object TransactionalEventPublisher {
  def apply[T: MqPublication](clock: Clock, eventStore: PendingEventStore): Publisher[ConnectionIO, T] =
    new SingleTransactionalEventPublisher[T](clock, eventStore)

  def multi[T <: Coproduct](clock: Clock, eventStore: PendingEventStore)(implicit @implicitNotFound("Could not find publication for all given messages in ${T}") ev: shapeless.ops.coproduct.Folder.Aux[EventPublisherMapper.type, T, (Topic, String)]): Publisher[ConnectionIO, T] =
    new MultiTransactionalEventPublisher[T](clock, eventStore)
}