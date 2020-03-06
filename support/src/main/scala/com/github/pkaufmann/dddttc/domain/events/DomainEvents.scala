package com.github.pkaufmann.dddttc.domain.events

trait Publisher[F[_], T] {
  def publish(event: T): F[Unit]
}

trait Subscription[F[_], T] {
  def on(m: T => F[_]): Unit
}