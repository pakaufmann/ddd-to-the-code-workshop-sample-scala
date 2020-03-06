package com.github.pkaufmann.dddttc.rental.infrastructure

import com.github.pkaufmann.dddttc.infrastructure.web.ErrorHandler
import org.http4s.dsl.io._
import org.http4s.twirl._

package object web {
  def defaultErrorHandler = ErrorHandler.onError(_ => InternalServerError(rental.html.error("The server encountered an unexpected error")))
}
