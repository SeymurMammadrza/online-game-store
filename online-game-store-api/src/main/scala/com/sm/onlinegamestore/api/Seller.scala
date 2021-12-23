package com.sm.onlinegamestore.api

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._

final case class Seller(sellerId: String, var games: Seq[Game])

object Seller {
  def empty: Seller = Seller("", Seq.empty)

  implicit val gameFormat: Format[Game] = Json.format
  implicit val format: Format[Seller] = Json.format[Seller]


  implicit val sellerReads: Reads[Seller] = (
    (JsPath \ "sellerId").read[String] and
      (JsPath \ "games").read[Seq[Game]]) (Seller.apply _)

  implicit val sellerWrites: Writes[Seller] = (
    (JsPath \ "sellerId").write[String] and
      (JsPath \ "games").write[Seq[Game]]) (unlift(Seller.unapply))
}

final case class ListSellers(list: List[Seller])

object ListSellers {
  implicit val gameFormat: Format[Game] = Json.format
  implicit val format: Format[Seller] = Json.format[Seller]
  implicit val SellersFormat: Format[ListSellers] = Json.format[ListSellers]
}

final case class SeqSellers(seq: Seq[Seller])

object SeqSellers {
  implicit val gameFormat: Format[Game] = Json.format
  implicit val format: Format[Seller] = Json.format[Seller]
  implicit val SellersFormat: Format[SeqSellers] = Json.format[SeqSellers]

}
