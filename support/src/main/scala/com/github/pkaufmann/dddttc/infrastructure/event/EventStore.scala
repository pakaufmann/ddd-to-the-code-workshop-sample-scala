package com.github.pkaufmann.dddttc.infrastructure.event

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.util.fragments._

object EventStore {
  type GetUnsent[F[_]] = (Int) => F[List[PendingDomainEvent]]

  type Store[F[_]] = PendingDomainEvent => F[Unit]

  type RemoveSent[F[_]] = List[PendingDomainEvent] => F[Unit]
}

object PendingEventStore {
  import EventStore._

  val getUnsent: GetUnsent[ConnectionIO] = {
    count => {
      sql"SELECT id, topic, payload, published_at, trace FROM domain_event ORDER BY published_at ASC LIMIT $count"
        .query[PendingDomainEvent]
        .stream
        .compile
        .toList
    }
  }

  val store: Store[ConnectionIO] = {
    event => {
      sql"INSERT INTO domain_event (id, topic, payload, published_at, trace) VALUES (${event.id}, ${event.topic}, ${event.payload}, ${event.publishedAt}, ${event.trace})"
        .update
        .run
        .map(_ => ())
    }
  }

  val removeSent: RemoveSent[ConnectionIO] = {
    unsent => {
      val unsentIn = unsent.map(_.id).toNel.map(in(fr"id", _))
      val q = fr"DELETE FROM domain_event" ++ whereAndOpt(unsentIn)
      q.update
        .run
        .map(_ => ())
    }
  }
}
