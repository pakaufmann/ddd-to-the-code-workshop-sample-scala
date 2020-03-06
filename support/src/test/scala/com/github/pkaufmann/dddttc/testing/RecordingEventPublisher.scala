package com.github.pkaufmann.dddttc.testing

import com.github.pkaufmann.dddttc.domain.events.Publisher
import shapeless.{Inl, Inr}

import scala.annotation.tailrec
import scala.reflect.ClassTag

class RecordingEventPublisher[F[_], T](other: Publisher[F, T]) extends Publisher[F, T] {
  var recordedEvents = List.empty[Any]

  override def publish(event: T): F[Unit] = {
    recordedEvents = add(recordedEvents, event)
    other.publish(event)
  }

  def getRecorded[F](implicit t: ClassTag[F]): Option[F] = {
    recordedEvents
      .find(_.getClass == t.runtimeClass)
      .map(_.asInstanceOf[F])
  }

  @tailrec
  private def add(recordedEvents: List[Any], event: Any): List[Any] = event match {
    case Inl(e) => add(recordedEvents, e)
    case Inr(e) => add(recordedEvents, e)
    case e => recordedEvents :+ e
  }
}
