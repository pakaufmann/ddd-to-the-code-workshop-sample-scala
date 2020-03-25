package com.github.pkaufmann.dddttc.registration.infrastructure

import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.web.ErrorHandler
import org.http4s.twirl._
import org.http4s.{Response, Status}

package object web {
  def defaultErrorHandler[F[_] : Sync] = ErrorHandler.onError(_ => Response[F](status = Status.InternalServerError).withEntity(registration.html.error("The server encountered an unexpected error")))
}
