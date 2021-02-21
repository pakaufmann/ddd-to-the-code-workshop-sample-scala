package com.github.pkaufmann.dddttc.rental

import cats.data.NonEmptyList
import cats.effect.internals.{IOContextShift, PoolUtils}
import cats.effect.{Clock, ContextShift, ExitCode, IO, IOApp, Sync}
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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import javax.jms.ConnectionFactory
import javax.naming.InitialContext
import javax.naming.Context
import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val config = if (args.contains("local")) {
      ConfigSource.resources("application-local.conf").loadOrThrow[ApplicationConfig]
    } else {
      ConfigSource.resources("application.conf").loadOrThrow[ApplicationConfig]
    }

    Persistence.initDb[IO](
      config.driver,
      config.url,
      config.user,
      config.password
    ).use { implicit xa =>
      implicit val conIOClock = Clock.create[ConnectionIO]

      def instant[F[_]](implicit S: Sync[F]) = S.delay(java.time.Clock.systemUTC().instant())

      val connFactory = if (config.brokerType == "local") {
        new ActiveMQConnectionFactory(config.brokerUrl)
      } else {
        AzureConnectionFactory(config.brokerUrl, config.brokerPassword)
      }

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
        .andThen(_.transact(xa))

      val publisherContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))
      val subscriberContext = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

      for {
        _ <- Persistence.initializeDb(addBike)
        publications <- publishLoop.start(publisherContext)
        subscriptions <- List(
          MqEventSubscriber.bind[IO, BookingCompletedEvent](
            connFactory,
            bookingCompletedHandler
          ),
          MqEventSubscriber.bind[IO, UserRegistrationCompletedMessage](
            connFactory,
            userRegistrationCompletedHandler
          )
        ).traverse(_.start(subscriberContext))
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
