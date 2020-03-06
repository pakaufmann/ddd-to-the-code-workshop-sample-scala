package com.github.pkaufmann.dddttc.infrastructure.event

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.util.fragments._

class PendingEventStore {
  def loadUnsent(count: Int): ConnectionIO[List[PendingDomainEvent]] = {
    sql"SELECT id, topic, payload, published_at FROM domain_event ORDER BY published_at ASC LIMIT $count"
      .query[PendingDomainEvent]
      .stream
      .compile
      .toList
  }

  def store(event: PendingDomainEvent): ConnectionIO[Unit] = {
    sql"INSERT INTO domain_event (id, topic, payload, published_at) VALUES (${event.id}, ${event.topic}, ${event.payload}, ${event.publishedAt})"
      .update
      .run
      .map(_ => ())
  }

  def removeSent(unsent: List[PendingDomainEvent]): ConnectionIO[Unit] = {
    val unsentIn = unsent.map(_.id).toNel.map(in(fr"id", _))
    val q = fr"DELETE FROM domain_event" ++ whereAndOpt(unsentIn)
    q.update
      .run
      .map(_ => ())
  }
}
