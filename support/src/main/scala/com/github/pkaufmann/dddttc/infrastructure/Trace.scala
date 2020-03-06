package com.github.pkaufmann.dddttc.infrastructure

import java.util.UUID

case class Trace(id: String)

object Trace {
  def apply(): Trace = {
    new Trace(UUID.randomUUID().toString)
  }
}