package com.sm.onlinegamestore.api

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._


case class Game(id: String, name: String, genre: String, price: Int)

object Game {

  def optionalApply(gameId: String, name: Option[String], genre: Option[String], price: Option[Int]): Option[Game] = {
    for {
      nameValue <- name if name.isDefined
      genreValue <- genre if genre.isDefined
      priceValue <- price if price.isDefined
    } yield Game(gameId, nameValue, genreValue, priceValue)
  }

  def empty: Game = Game("", "", "", 0)

  implicit val gameFormat: Format[Game] = Json.format


  implicit val gameReads: Reads[Game] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "genre").read[String] and
      (JsPath \ "price").read[Int]) (Game.apply _)

  implicit val gameWrites: Writes[Game] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "genre").write[String] and
      (JsPath \ "price").write[Int]) (unlift(Game.unapply))


  case class GameList(list : List[Game])

  object GameList{
    implicit val gameFormat: Format[Game] = Json.format
    implicit val gameListFormat: Format[GameList] = Json.format[GameList]

  }

  case class GameSeq(seq: Seq[Game])

  object GameSeq {
    implicit val gameFormat: Format[Game] = Json.format
    implicit val gameSeqFormat: Format[GameSeq] = Json.format[GameSeq]

  }
}