package com.sm.onlinegamestore.impl

import akka.actor.typed.ActorRef
import com.sm.onlinegamestore.api.Seller
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{Format, JsResult, JsValue, Json}

object Commands {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class Summary(sellers: scala.collection.immutable.Seq[Seller], operationStatus: Boolean)

  object Summary {
    implicit val format: Format[Summary] = Json.format
  }

  sealed trait Confirmation

  case object Confirmation {

    implicit val format: Format[Confirmation] = new Format[Confirmation] {
      override def reads(json: JsValue): JsResult[Confirmation] = {
        if ((json \ "reason").isDefined)
          Json.fromJson[Rejected](json)
        else {
          Json.fromJson[Accepted](json)
        }
      }

      override def writes(o: Confirmation): JsValue = {
        o match {
          case acc: Accepted => Json.toJson(acc)
          case rej: Rejected => Json.toJson(rej)
        }
      }
    }
  }

  case class CreateSeller(sellerId: String, replyTo: ActorRef[Confirmation])
    extends Command

  final case class AddGameToSeller(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int], replyTo: ActorRef[Confirmation]) extends Command

  final case class RemoveGameFromSeller(sellerId: String, gameId: String, replyTo: ActorRef[Confirmation]) extends Command

  final case class UpdateGameOfSeller(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int], replyTo: ActorRef[Confirmation]) extends Command

  final case class FindGameBySellerAndGameId(sellerId: String, gameId: String, replyTo: ActorRef[Confirmation]) extends Command

  final case class FindSellerBySellerId(sellerId: String, replyTo: ActorRef[Confirmation]) extends Command

  case class Accepted(summary: Summary) extends Confirmation

  object Accepted {
    implicit val format: Format[Accepted] = Json.format
  }

  case class Rejected(reason: String) extends Confirmation

  object Rejected {
    implicit val format: Format[Rejected] = Json.format
  }

}
