package com.github.pkaufmann.dddttc.rental

import cats.data.NonEmptyList
import cats.effect.{Clock, ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.github.pkaufmann.dddttc.infrastructure.event._
import com.github.pkaufmann.dddttc.infrastructure.implicits._
import com.github.pkaufmann.dddttc.infrastructure.web._
import com.github.pkaufmann.dddttc.rental.application.domain.bike.BookingCompletedEventHandler
import com.github.pkaufmann.dddttc.rental.application.domain.booking.BookingCompletedEvent
import com.github.pkaufmann.dddttc.rental.application.{BikeService, BookingService, UserService}
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener
import com.github.pkaufmann.dddttc.rental.infrastructure.event.UserRegistrationCompletedMessageListener.Message.UserRegistrationCompletedMessage
import com.github.pkaufmann.dddttc.rental.infrastructure.event.implicits._
import com.github.pkaufmann.dddttc.rental.infrastructure.generator.Generator
import com.github.pkaufmann.dddttc.rental.infrastructure.persistence.{InMemoryBikeRepository, JdbcBookingRepository, JdbcUserRepository, Persistence}
import com.github.pkaufmann.dddttc.rental.infrastructure.web.{RentalController, defaultErrorHandler}
import doobie.free.connection.ConnectionIO
import org.apache.activemq.ActiveMQConnectionFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import doobie.implicits._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Persistence.initDb[IO]().use { implicit xa =>
      implicit val conIOClock = Clock.create[ConnectionIO]

      val config = ConfigSource.default.loadOrThrow[ApplicationConfig]

      def instant[F[_]](implicit S: Sync[F]) = S.delay(java.time.Clock.systemUTC().instant())

      val connFactory = new ActiveMQConnectionFactory(config.brokerUrl)

      val publish = MqEventPublisher.publish(
        PendingEventStore.getUnsent, PendingEventStore.removeSent
      )

      val publishLoop = MqSession.withSession[IO](connFactory)(publish.transact(xa))

      val getBike = BikeService.getBike(InMemoryBikeRepository.get[IO])
      val listBikes = BikeService.listBikes(InMemoryBikeRepository.findAll[IO])
      val addBike = BikeService.addBike(InMemoryBikeRepository.add[IO])

      val addUser = UserService.addUser(JdbcUserRepository.add)

      val bookBike = BookingService.bookBike[ConnectionIO](
        InMemoryBikeRepository.get[ConnectionIO],
        InMemoryBikeRepository.update[ConnectionIO],
        JdbcUserRepository.get,
        JdbcBookingRepository.add,
        instant[ConnectionIO],
        Generator.uuid[ConnectionIO]
      )

      val completeBooking = BookingService.completeBooking(
        JdbcBookingRepository.get,
        JdbcBookingRepository.update,
        TransactionalEventPublisher.single(PendingEventStore.store),
        instant[ConnectionIO]
      )
      val listBookings = BookingService.listBookings(JdbcBookingRepository.findAll)

      val bookingCompletedHandler = BookingCompletedEventHandler[IO](
        InMemoryBikeRepository.get[IO],
        InMemoryBikeRepository.update[IO]
      )

      val userRegistrationCompletedHandler = UserRegistrationCompletedMessageListener(addUser)

      for {
        _ <- Persistence.initializeDb(addBike)
        publications <- publishLoop.start
        subscriptions <- List(
          MqEventSubscriber.bind[IO, BookingCompletedEvent](
            connFactory,
            bookingCompletedHandler
          ),
          MqEventSubscriber.bind[IO, UserRegistrationCompletedMessage](
            connFactory,
            userRegistrationCompletedHandler.andThen(_.transact(xa))
          )
        ).traverse(_.start)
        server <- Server
          .create[IO](
            config.port,
            defaultErrorHandler[IO],
            "/rental" -> (
              RentalController.listBikes[IO](listBikes) <+>
                RentalController.getBike[IO](getBike) <+>
                RentalController.bookBike[IO](bookBike.andThen(_.transact(xa))) <+>
                RentalController.completeBooking[IO](completeBooking.andThen(_.transact(xa))) <+>
                RentalController.listBookings[IO](listBookings.transact(xa)))
          )
          .use(_ => IO.never)
          .as(ExitCode.Success)
          .start
        exitCode <- NonEmptyList(
          server,
          subscriptions :+ publications.map(_ => ExitCode.Error)
        ).joinAll
      } yield exitCode
    }
  }
}
