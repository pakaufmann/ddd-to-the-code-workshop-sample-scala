package com.github.pkaufmann.dddttc.infrastructure

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.effect.{ContextShift, Fiber, IO, Sync}
import cats.implicits._
import org.http4s.{EntityDecoder, UrlForm}
import shapeless.labelled.{FieldType, field}
import shapeless.{::, CNil, HList, HNil, LabelledGeneric, Lazy, Witness}

package object web {

  implicit class FiberJoiner[A](val fibers: NonEmptyList[Fiber[IO, A]]) extends AnyVal {
    def joinAll(implicit cs: ContextShift[IO]): IO[A] = {
      fibers
        .map(_.join)
        .reduceLeft(IO.race(_, _).map(_.merge))
        .guarantee(fibers.traverse(_.cancel) *> IO.unit)
    }
  }

  trait UrlFormDecoder[A] {
    def decode(form: UrlForm): ValidatedNel[String, A]
  }

  object UrlFormDecoder extends urlFormDecoders0 {
    implicit def optionDecoder[T](implicit d: UrlFormDecoder[T]): UrlFormDecoder[Option[T]] = decoder {
      case s: UrlForm if s.values.size == 1 =>
        val blank = s.values.head._2.headOption.forall(_.isBlank)
        if(!blank) {
          d.decode(s).map(Some.apply)
        } else {
          Valid(None)
        }
      case _ => Valid(None)
    }

    implicit val stringDecoder: UrlFormDecoder[String] = decoder {
      case s: UrlForm if s.values.size == 1 => s.values.head._2.headOption.map(Valid.apply)
        .getOrElse(Invalid(NonEmptyList.one(s"String not found $s")))
      case prop => Invalid(NonEmptyList.one(s"String not found $prop"))
    }

    def urlFormDecoder[T](in: UrlForm => ValidatedNel[String, T]): UrlFormDecoder[T] = decoder(in)

    def createDecoder[T](in: String => T): UrlFormDecoder[T] = decoder {
      case s if s.values.size == 1 => s.values.head._2.headOption.map(v => Valid(in(v)))
        .getOrElse(Invalid(NonEmptyList.one(s"Not found $s")))
      case prop => Invalid(NonEmptyList.one(s"Not found $prop"))
    }

    def apply[F[_] : Sync, A](implicit dcr: UrlFormDecoder[A]): EntityDecoder[F, A] =
      FormDecoder.decodeForm(dcr.decode)
  }

  def decoder[A](f: UrlForm => ValidatedNel[String, A]): UrlFormDecoder[A] = new UrlFormDecoder[A] {
    def decode(form: UrlForm): ValidatedNel[String, A] = f(form)
  }

  trait urlFormDecoders0 {
    implicit val hNilValueDecoder: UrlFormDecoder[HNil] = decoder(_ => Valid(HNil))

    implicit val cNilValueDecoder: UrlFormDecoder[CNil] = decoder(_ => Invalid(NonEmptyList.one("Coproduct value not found")))

    implicit def hListValueDecoder[K <: Symbol, H, T <: HList]
    (implicit witness: Witness.Aux[K],
     hDecoder: Lazy[UrlFormDecoder[H]],
     tDecoder: UrlFormDecoder[T]): UrlFormDecoder[FieldType[K, H] :: T] = {
      val name = witness.value.name
      decoder { prop =>
        val filtered = UrlForm(prop.values.find(_._1 == name).map(a => Map(a)).getOrElse(Map.empty))
        val rest = UrlForm(prop.values.filter(_._1 != name))
        val decoded = hDecoder.value.decode(filtered).leftMap(_.map(e => s"$name: $e"))
        val decodedRest = tDecoder.decode(rest)
        (decoded, decodedRest).mapN((f, s) => field[K](f) :: s)
      }
    }

    implicit def genericValueDecoder[A, H]
    (implicit generic: LabelledGeneric.Aux[A, H],
     hDecoder: Lazy[UrlFormDecoder[H]]): UrlFormDecoder[A] = decoder { prop =>
      hDecoder.value.decode(prop).map(generic.from)
    }
  }

}
