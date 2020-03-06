package com.github.pkaufmann.dddttc.infrastructure.web

import cats.effect.IO
import org.http4s._
import org.log4s._

import scala.util.control.NonFatal

object ErrorHandler {
  private val logger = getLogger

  def onError(r: Throwable => IO[Response[IO]]): Request[IO] => PartialFunction[Throwable, IO[Response[IO]]] = errorHandling(r)

  def errorHandling(response: Throwable => IO[Response[IO]]): Request[IO] => PartialFunction[Throwable, IO[Response[IO]]] = req => {
    case mf: MessageFailure =>
      for {
        _ <- IO.delay(logger.error(mf)("Failed to handle incoming request"))
      } yield mf.toHttpResponse[IO](req.httpVersion)
    case NonFatal(t) =>
      for {
        _ <- IO.delay(logger.error(t)(s"Unexpected error while handling request: $req"))
        e <- response(t)
      } yield e
  }
}
