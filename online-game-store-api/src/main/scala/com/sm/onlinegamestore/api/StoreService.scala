package com.sm.onlinegamestore.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.JsValueMessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json._

object StoreService {
  val TOPIC_NAME = "store"
}

trait StoreService extends Service {

  final override def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("store")
      .withCalls(
        restCall(Method.GET, "/seller/:sellerId", findSellerBySellerId _),
        restCall(Method.GET, "/seller/:sellerId/games/:gameId", findGameBySellerAndGameId _),
        restCall(Method.POST, "/seller/:sellerId/games/create", addGameToSeller _),
        restCall(Method.POST, "/seller/create", createSeller _),
        restCall(Method.DELETE, "/seller/:sellerId/games/:gameId/delete", removeGameFromSeller _),
        restCall(Method.PATCH, "/seller/:sellerId/games/:gameId/update", updateGameOfSeller _),
      ).withTopics(
      topic(StoreService.TOPIC_NAME, storeTopic)
    )
      .withAutoAcl(true)
    // @formatter:on
  }

  def storeTopic: Topic[EventPublished]


  def findGameBySellerAndGameId(sellerId: String, gameId: String): ServiceCall[Game, View]

  def findSellerBySellerId(sellerId: String): ServiceCall[NotUsed, View]

  def addGameToSeller(sellerId: String): ServiceCall[GamePayload, View]

  def createSeller(): ServiceCall[NotUsed, View]

  def removeGameFromSeller(sellerId: String, gameId: String): ServiceCall[NotUsed, View]

  def updateGameOfSeller(sellerId: String, gameId: String): ServiceCall[GamePayload, View]

}


final case class View(sellers: scala.collection.immutable.Seq[Seller]) {
  def sellersFromView: scala.collection.immutable.Seq[Seller] = this.sellers
}

object View {
  implicit val gameFormat: Format[Game] = Json.format
  implicit val sellerFormat: Format[Seller] = Json.format[Seller]
  implicit val format: Format[View] = Json.format
}

