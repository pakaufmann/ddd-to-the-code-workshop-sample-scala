package com.github.pkaufmann.dddttc.rental

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats.tagless.Derive
import com.github.pkaufmann.dddttc.infrastructure.event.{EventPublisher, MqEventSubscriber, MqSession, PendingEventStore, TransactionalEventPublisher}
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.BookingCompletedEventHandler
import com.github.pkaufmann.dddttc.rental.application.domain.booking.{BookBikeService, BookingCompletedEvent}
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService, UserService}
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.rental.infrastructure.persistence.{InMemoryBikeRepository, JdbcBookingRepository, JdbcUserRepository, Persistence}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.{RentalController, defaultErrorHandler}
import com.github.pkaufmann.dddttc.rental.infrastructure.event.implicits._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.tagless.implicits._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val eventPublisherFunctorK = Derive.functorK[EventPublisher]
    implicit val bookingServiceFunctorK = Derive.functorK[BookingService]
    implicit val bikeServiceFunctorK = Derive.functorK[BikeService]

    Persistence.initDb[IO]().use { implicit xa =>
      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      val clock = java.time.Clock.systemUTC()

      val bikeRepository = new InMemoryBikeRepository[ConnectionIO]()
      val bookingRepository = new JdbcBookingRepository()
      val userRepository = new JdbcUserRepository()

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)
      val pendingEventPublisher = EventPublisher.fromStore(PendingEventStore)
      val eventSubscriber = new MqEventSubscriber(connFactory)

      val bookBikeService = new BookBikeService(
        bikeRepository,
        bookingRepository,
        userRepository,
        clock
      )

      val bookingService = BookingService[ConnectionIO](
        bookBikeService,
        bookingRepository,
        TransactionalEventPublisher.single[ConnectionIO, BookingCompletedEvent](clock, PendingEventStore),
        clock
      )

      val bikeService = BikeService(bikeRepository)
      val userService = UserService(userRepository)

      val publishLoop = MqSession.withSession(connFactory)(pendingEventPublisher.publish().mapK(xa.trans))

      for {
        _ <- Persistence.initializeDb(bikeService).transact(xa)
        publications <- publishLoop.start
        subscriptions <- List(
          eventSubscriber.bind[IO, BookingCompletedEvent](BookingCompletedEventHandler.onBookingCompleted(bikeRepository).andThen(_.transact(xa))),
          eventSubscriber.bind[IO, UserRegistrationCompletedMessage](UserRegistrationCompletedMessageListener(userService).andThen(_.transact(xa)))
        ).traverse(_.start)
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
