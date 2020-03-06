package com.github.pkaufmann.dddttc.accounting.infrastructure

import com.github.pkaufmann.dddttc.accounting.application.domain.{UserId, Wallet}
import com.github.pkaufmann.dddttc.accounting.infrastructure.json.implicits._
import doobie.Meta
import io.circe.parser.decode
import io.circe.syntax._

package object persistence {
  implicit val userIdMeta: Meta[UserId] = Meta[String].timap(UserId.apply)(_.value)
  implicit val walletMeta: Meta[Wallet] = Meta[String].timap(u => decode[Wallet](u).left.map(e => throw e).merge)(_.asJson.noSpaces)
}
