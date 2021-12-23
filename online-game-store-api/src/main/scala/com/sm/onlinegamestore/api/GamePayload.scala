package com.sm.onlinegamestore.api

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsPath, Json, Reads, Writes}

case class GamePayload(name: Option[String], genre: Option[String], price: Option[Int])

object GamePayload {

  implicit val gamesFormat: Format[GamePayload] = Json.format

  implicit val gameReads: Reads[GamePayload] = (
    (JsPath \ "name").readNullable[String] and
      (JsPath \ "genre").readNullable[String] and
      (JsPath \ "price").readNullable[Int]) (GamePayload.apply _)

  implicit val gameWrites: Writes[GamePayload] = (
    (JsPath \ "name").writeNullable[String] and
      (JsPath \ "genre").writeNullable[String] and
      (JsPath \ "price").writeNullable[Int]) (unlift(GamePayload.unapply))
}