package com.sm.onlinegamestore.impl

import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import Commands._
import Events._
import Store.typeKey
import com.sm.onlinegamestore.api.{EventPublished, Game, GamePayload, StoreService, View}
import com.sm.onlinegamestore.impl.Commands.{Command, Confirmation, Summary}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class StoreServiceImpl(
                        clusterSharding: ClusterSharding,
                        persistentEntityRegistry: PersistentEntityRegistry
                      )(implicit ec: ExecutionContext)
  extends StoreService {

  /**
   * Looks up the entity for the given ID.
   */
  private def entityRef(id: String): EntityRef[Command] = {
    clusterSharding.entityRefFor(typeKey, id)
  }

  implicit val timeout: Timeout = Timeout(5.seconds)

  def replace[A](element: A, seq: scala.collection.immutable.Seq[A]): scala.collection.immutable.Seq[A] = seq.map(x => if (x.equals(element)) element else x)


  override def createSeller(): ServiceCall[NotUsed, View] = ServiceCall { _ =>
    val newSellerId = UUID.randomUUID.toString
    entityRef(newSellerId).ask[Confirmation](reply => CreateSeller(newSellerId, reply))
      .map { confirmation => confirmationToResult(confirmation) }
  }

  override def findSellerBySellerId(sellerId: String): ServiceCall[NotUsed, View] = ServiceCall { _ =>
    entityRef(sellerId)
      .ask[Confirmation](reply => FindSellerBySellerId(sellerId, reply)).map { confirmation => confirmationToResult(confirmation)
    }
  }

  override def findGameBySellerAndGameId(sellerId: String, gameId: String): ServiceCall[Game, View] = ServiceCall { _ =>
    entityRef(sellerId)
      .ask[Confirmation](reply => FindGameBySellerAndGameId(sellerId, gameId, reply))
      .map { confirmation => confirmationToResult(confirmation) }
  }


  override def addGameToSeller(sellerId: String): ServiceCall[GamePayload, View] = ServiceCall { gamePayload =>
    val newGameId = UUID.randomUUID.toString
    entityRef(sellerId)
      .ask(reply => AddGameToSeller(sellerId, newGameId, gamePayload.name, gamePayload.genre, gamePayload.price, reply))
      .map { confirmation => confirmationToResult(confirmation) }
  }


  override def removeGameFromSeller(sellerId: String, gameId: String): ServiceCall[NotUsed, View] = ServiceCall { _ =>
    entityRef(sellerId)
      .ask(reply => RemoveGameFromSeller(sellerId, gameId, reply))
      .map { confirmation => confirmationToResult(confirmation) }
  }


  override def updateGameOfSeller(sellerId: String, gameId: String): ServiceCall[GamePayload, View] = ServiceCall { game =>
    entityRef(sellerId)
      .ask(reply => UpdateGameOfSeller(sellerId, gameId, game.name, game.genre, game.price, reply))
      .map { confirmation => confirmationToResult(confirmation) }
  }

  private def confirmationToResult(confirmation: Confirmation): View =
    confirmation match {
      case Accepted(summary) => convertToStore(summary)
      case Rejected(reason) => throw BadRequest(reason)
    }


  private def convertToStore(summary: Summary): View = View(summary.sellers)

  override def storeTopic: Topic[EventPublished] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(Events.Event.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(eventStream: EventStreamElement[Event]): EventPublished = {
    eventStream.event match {
      case SellerCreated(seller) => EventPublished(UUID.randomUUID().toString,SellerCreated(seller).toString)
      case GameAdded(sellerId, gameId, name, genre, price) =>
        EventPublished(UUID.randomUUID().toString,GameAdded(sellerId, gameId, name, genre, price).toString)
      case GameUpdated(sellerId, gameId, name, genre, price) =>
       EventPublished(UUID.randomUUID().toString,GameUpdated(sellerId, gameId, name, genre, price).toString)
      case GameRemoved(sellerId, gameId) =>
        EventPublished(UUID.randomUUID().toString,GameRemoved(sellerId, gameId).toString)
      case GameFound(sellerId, gameId) =>
        EventPublished(UUID.randomUUID().toString,GameFound(sellerId, gameId).toString)
      case SellerFound(sellerId) =>
        EventPublished(UUID.randomUUID().toString,SellerFound(sellerId).toString)
    }
  }
}