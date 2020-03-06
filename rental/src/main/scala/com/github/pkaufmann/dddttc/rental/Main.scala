package com.github.pkaufmann.dddttc.rental

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event.{MqEventSubscriber, PendingEventPublisher, PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.infrastructure.persistence.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.BookingCompletedEventHandler
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookBikeService
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService, UserService}
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener
import com.github.pkaufmann.dddttc.rental.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.rental.infrastructure.persistence.{InMemoryBikeRepository, JdbcBookingRepository, JdbcUserRepository, Persistence}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.{RentalController, defaultErrorHandler}
import doobie.free.connection.ConnectionIO
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Persistence.initDb().use { implicit xa =>
      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val bikeRepository = new InMemoryBikeRepository[ConnectionIO]()
      val bookingRepository = new JdbcBookingRepository()
      val userRepository = new JdbcUserRepository()

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)
      val eventStore = new PendingEventStore()
      val pendingEventPublisher = new PendingEventPublisher(eventStore, connFactory)
      val domainPublisher = TransactionalEventPublisher(clock, eventStore)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val bookBikeService = new BookBikeService(bikeRepository, bookingRepository, userRepository, clock)

      val bookingService = new BookingService(bookBikeService, bookingRepository, domainPublisher, clock)
      val bikeService = new BikeService(bikeRepository)
      val userService = new UserService(userRepository)

      BookingCompletedEventHandler.register(eventSubscriber.createSubscription(), bikeRepository)

      eventSubscriber.subscribe(UserRegistrationCompletedMessageListener(userService))

      for {
        _ <- Persistence.initializeDb(bikeService).transact
        publications <- pendingEventPublisher.start()
        subscriptions <- eventSubscriber.start()
        server <- Server
          .create(
            config.port,
            defaultErrorHandler,
            "/rental" -> RentalController.routes(bookingService, bikeService),
          )
          .start
        exitCode <- NonEmptyList(
          server,
          subscriptions :+ publications.map(_ => ExitCode.Error)
        ).joinAll
      } yield exitCode
    }
  }
}
