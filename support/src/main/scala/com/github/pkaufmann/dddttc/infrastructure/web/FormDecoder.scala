package com.github.pkaufmann.dddttc.infrastructure.web

import cats.Applicative
import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits._
import org.http4s._

object FormDecoder {
  def decodeForm[F[_] : Sync, T](decode: UrlForm => ValidatedNel[String, T]): EntityDecoder[F, T] = {
    UrlForm.entityDecoder[F].flatMapR { f =>
      DecodeResult(
        decode(f)
          .toEither
          .leftMap[DecodeFailure](e => {
            MalformedMessageBodyFailure(s"Could not decode form: ${e.mkString_(",")}")
          })
          .pure[F]
      )
    }
  }
}
