package com.github.pkaufmann.dddttc.infrastructure.event

import scala.util.Try

trait MqPubSub[T] extends MqPublication[T] with MqSubscription[T]

object MqPubSub {
  def apply[T](c: Topic, pub: T => String, sub: String => Try[T]): MqPubSub[T] = new MqPubSub[T] {
    override def topic(): Topic = c

    override def asPayload(in: T): String = pub(in)

    override def asObject(in: String): Try[T] = sub(in)
  }
}