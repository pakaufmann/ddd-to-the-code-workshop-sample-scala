package com.github.pkaufmann.dddttc.infrastructure.web

import java.util.concurrent.Executors

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.{Blocker, ContextShift, ExitCode, Fiber, IO, Timer}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.HttpMethodOverrider
import org.http4s.server.middleware.HttpMethodOverrider.{FormOverrideStrategy, HttpMethodOverriderConfig}
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.http4s.server.{Router, ServiceErrorHandler}
import org.http4s.{HttpRoutes, Method}


object Server {
  def create(port: Int, errorHandler: ServiceErrorHandler[IO], mappings: (String, HttpRoutes[IO])*)(implicit t: Timer[IO], cs: ContextShift[IO]) = {
    val blockingPool = Executors.newFixedThreadPool(4)
    val blocker = Blocker.liftExecutorService(blockingPool)

    BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(HttpMethodOverrider(
        Router(mappings :+ "/static" -> resourceService[IO](ResourceService.Config("/static", blocker)): _*).orNotFound,
        HttpMethodOverriderConfig(FormOverrideStrategy[IO, IO]("_method", FunctionK.lift[IO, IO](identity)), Set(Method.POST))
      ))
      .withServiceErrorHandler(errorHandler)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
