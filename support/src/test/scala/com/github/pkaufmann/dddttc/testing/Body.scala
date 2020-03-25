package com.github.pkaufmann.dddttc.testing

import cats.effect.SyncIO
import org.http4s.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Body {
  def read(resp: Response[SyncIO]): String = {
    resp.body.compile.toVector.unsafeRunSync().map(_.toChar).mkString
  }

  def readHtml(resp: Response[SyncIO]): Document = {
    Jsoup.parse(read(resp))
  }
}
