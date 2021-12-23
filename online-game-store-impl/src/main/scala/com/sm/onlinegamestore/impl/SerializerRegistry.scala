package com.sm.onlinegamestore.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import Commands.{Accepted, Confirmation, Rejected}
import Events._
import com.sm.onlinegamestore.api.Game.{GameList, GameSeq}
import com.sm.onlinegamestore.api.{Game, GamePayload, ListSellers, Seller, SeqSellers, View}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

object SerializerRegistry extends JsonSerializerRegistry {
  implicit val confFormat: Format[Confirmation] = Json.format

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
    JsonSerializer[Game],
    JsonSerializer[GamePayload],
    JsonSerializer[GameSeq],
    JsonSerializer[GameList],
    JsonSerializer[ListSellers],
    JsonSerializer[SeqSellers],
    JsonSerializer[View],
    JsonSerializer[Seller],
    JsonSerializer[Store],
    // the replies use play-json as well
    JsonSerializer[GameAdded],
    JsonSerializer[GameRemoved],
    JsonSerializer[GameUpdated],
    JsonSerializer[GameFound],
    JsonSerializer[SellerFound],
    JsonSerializer[SellerCreated],
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected]
  )
}
