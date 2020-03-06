package com.github.pkaufmann.dddttc.registration.infrastructure

import com.github.pkaufmann.dddttc.registration.application.domain.UserRegistration
import com.github.pkaufmann.dddttc.registration.infrastructure.json.implicits._
import doobie._
import io.circe.parser.decode
import io.circe.syntax._

package object persistence {
  implicit val walletMeta: Meta[UserRegistration] = Meta[String].timap(u => decode[UserRegistration](u).left.map(e => throw e).merge)(_.asJson.noSpaces)
}
