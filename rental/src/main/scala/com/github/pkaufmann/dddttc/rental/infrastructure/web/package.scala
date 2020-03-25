package com.github.pkaufmann.dddttc.rental.infrastructure

import cats.effect.Sync
import com.github.pkaufmann.dddttc.infrastructure.web.ErrorHandler
import org.http4s.{Response, Status}
import org.http4s.twirl._

package object web {
  def defaultErrorHandler[F[_] : Sync] = ErrorHandler.onError[F](_ => Response[F](status = Status.InternalServerError).withEntity(rental.html.error("The server encountered an unexpected error")))
}
