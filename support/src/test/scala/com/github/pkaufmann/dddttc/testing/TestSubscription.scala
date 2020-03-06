package com.github.pkaufmann.dddttc.testing

import cats.Id
import com.github.pkaufmann.dddttc.domain.events.Subscription

class TestSubscription[T]() extends Subscription[Id, T] {
  private var subscription: Option[T => Id[_]] = None

  override def on(m: T => Id[_]): Unit = subscription = Some(m)

  def send(m: T): Id[_] = subscription.map(s => s(m)).get
}
