package com.github.pkaufmann.dddttc.testing

import cats.Eq
import org.scalatest.matchers.{MatchResult, Matcher}
import shapeless.{Generic, HList, LabelledGeneric}

import scala.language.implicitConversions

object AggregateBuilder {
  def create[T](implicit ev: Generic[T]): ev.Repr => T = {
    e: ev.Repr => Generic[T].from(e)
  }

  implicit class Back[L <: HList](private val in: L) extends AnyVal {
    def back[T](implicit ev: LabelledGeneric.Aux[T, L]): T = {
      ev.from(in)
    }
  }

  implicit class AsRepr[T](private val in: T) extends AnyVal {
    def change(implicit ev: LabelledGeneric[T]): ev.Repr = ev.to(in)
  }

  trait AggregateMatchers {

    class EqvMatcher[T](expected: T)(implicit eq: Eq[T]) extends Matcher[T] {
      override def apply(left: T): MatchResult = {
        MatchResult(
          eq.eqv(expected, left),
          s"Aggregate\n$left did not match\n$expected",
          s"Aggregate $left matched $expected"
        )
      }
    }

    def eqv[T: Eq](expected: T) = new EqvMatcher(expected)
  }

  object AggregateMatchers extends AggregateMatchers

}