package com.github.pkaufmann.dddttc.infrastructure.event

import scala.util.Try

object MqPubSub {
  def create[T](c: Topic, pub: T => String, sub: String => Try[T]): MqPublication[T] with MqSubscription[T] =
    new MqPublication[T] with MqSubscription[T] {
      override val topic: Topic = c

      override def asPayload(in: T): String = pub(in)

      override def asObject(in: String): Try[T] = sub(in)
    }
}