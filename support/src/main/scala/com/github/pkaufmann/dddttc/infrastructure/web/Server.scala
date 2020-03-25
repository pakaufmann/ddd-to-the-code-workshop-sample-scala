package com.github.pkaufmann.dddttc.infrastructure.web

import java.util.concurrent.Executors

import cats.arrow.FunctionK
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.HttpMethodOverrider
import org.http4s.server.middleware.HttpMethodOverrider.{FormOverrideStrategy, HttpMethodOverriderConfig}
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.http4s.server.{Router, ServiceErrorHandler}
import org.http4s.{HttpRoutes, Method}


object Server {
  def create[F[_] : ConcurrentEffect](port: Int, errorHandler: ServiceErrorHandler[F], mappings: (String, HttpRoutes[F])*)(implicit t: Timer[F], cs: ContextShift[F]) = {
    val blockingPool = Executors.newFixedThreadPool(4)
    val blocker = Blocker.liftExecutorService(blockingPool)

    BlazeServerBuilder[F]
      .bindHttp(port, "localhost")
      .withHttpApp(HttpMethodOverrider(
        Router(mappings :+ "/static" -> resourceService[F](ResourceService.Config("/static", blocker)): _*).orNotFound,
        HttpMethodOverriderConfig(FormOverrideStrategy("_method", FunctionK.id[F]), Set(Method.POST))
      ))
      .withServiceErrorHandler(errorHandler)
      .resource
  }
}
