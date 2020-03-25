package com.github.pkaufmann.dddttc.infrastructure.web

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.log4s._

import scala.util.control.NonFatal

object ErrorHandler {
  private val logger = getLogger

  def onError[F[_] : Sync](r: Throwable => Response[F]): Request[F] => PartialFunction[Throwable, F[Response[F]]] = errorHandling(r)

  def errorHandling[F[_] : Sync](response: Throwable => Response[F]): Request[F] => PartialFunction[Throwable, F[Response[F]]] =
    req => {
      case mf: MessageFailure =>
        for {
          _ <- Sync[F].delay(logger.error(mf)("Failed to handle incoming request"))
        } yield mf.toHttpResponse[F](req.httpVersion)
      case NonFatal(t) =>
        for {
          _ <- Sync[F].delay(logger.error(t)(s"Unexpected error while handling request: $req"))
        } yield response(t)
    }
}
