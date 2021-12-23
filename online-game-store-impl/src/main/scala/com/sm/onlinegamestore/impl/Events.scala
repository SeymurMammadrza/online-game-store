package com.sm.onlinegamestore.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import com.sm.onlinegamestore.api.Seller
import play.api.libs.json.{Format, Json}

object Events {
  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventTag[Event] = AggregateEventTag[Event]

  }

  case class SellerCreated(seller: Seller) extends Event

  object SellerCreated {
    implicit val format: Format[SellerCreated] = Json.format
  }

  case class GameAdded(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int]) extends Event

  object GameAdded {
    implicit val format: Format[GameAdded] = Json.format
  }

  case class GameRemoved(sellerId: String, gameId: String) extends Event

  object GameRemoved {
    implicit val format: Format[GameRemoved] = Json.format
  }

  case class GameUpdated(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int]) extends Event

  object GameUpdated {
    implicit val format: Format[GameUpdated] = Json.format
  }

  final case class GameFound(sellerId: String, gameId: String) extends Event

  object GameFound {
    implicit val format: Format[GameFound] = Json.format
  }

  final case class SellerFound(sellerId: String) extends Event

  object SellerFound {
    implicit val format: Format[SellerFound] = Json.format
  }
}
