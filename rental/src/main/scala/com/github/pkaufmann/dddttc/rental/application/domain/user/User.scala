package com.github.pkaufmann.dddttc.rental.application.domain.user

import com.github.pkaufmann.dddttc.stereotypes.{Aggregate, AggregateFactory, AggregateId}

@Aggregate
case class User private(@AggregateId id: UserId)

private[application] object User {
  @AggregateFactory
  def apply(id: UserId): User = new User(id)
}