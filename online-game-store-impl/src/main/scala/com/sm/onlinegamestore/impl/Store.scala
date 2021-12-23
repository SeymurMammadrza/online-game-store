package com.sm.onlinegamestore.impl

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import Commands._
import Events._
import com.sm.onlinegamestore.api.{Game, ListSellers, Seller, SeqSellers}
import play.api.libs.json._

import scala.collection.immutable.Seq


final case class Store(sellers: Seq[Seller]) {

  def convert[T](sq: collection.mutable.Seq[T]): collection.immutable.Seq[T] =
    collection.immutable.Seq[T](sq: _*)

  var collectionOfSellers: scala.collection.mutable.Seq[Seller] = scala.collection.mutable.Seq.empty

  def sellerSeqSearch(seller: Seller, seq: Seq[Seller]): Option[Seller] =
    seq.find(sellerSearched => sellerSearched.sellerId == seller.sellerId)

  def gameSeqSearch(game: Game, seq: scala.Seq[Game]): Option[Game] =
    seq.find(gameSearched => gameSearched.id == game.id)

  def replaceSeller(seller: Seller, seq: Seq[Seller]): Seq[Seller] = {
    sellerSeqSearch(seller, seq) match {
      case Some(value) =>
        val newSeq = seq.filterNot(_.sellerId == seller.sellerId)
        val newSeller = value.copy(seller.sellerId, seller.games)
        val updatedSeq = newSeq :+ newSeller
        print(s"updatedSeq: $updatedSeq")
        updatedSeq
      case None => seq
    }
  }

  def replaceGame(game: Game, seq: scala.Seq[Game]): scala.Seq[Game] = {
    gameSeqSearch(game, seq) match {
      case Some(value) =>
        val newSeq = seq.filterNot(_.id == game.id)
        val newSeller = value.copy(game.id, game.name, game.genre, game.price)
        val updatedSeq = newSeq :+ newSeller
        print(s"updatedSeq: $updatedSeq")
        updatedSeq
      case None => seq
    }
  }

  def findSellerById(sellerId: String): Option[Seller] = {
    sellers.find(_.sellerId == sellerId)
  }

  def findGameById(sellerId: String, gameId: String): Option[Game] = {
    findSellerById(sellerId) match {
      case Some(value) => value.games.find(_.id == gameId)
      case None => None
    }
  }

  def toSummary(store: Store): Summary = {
    Summary(store.sellers, operationStatus = true)
  }

  def toCollection(seller: Seller): scala.collection.mutable.Seq[Seller] = {
    collectionOfSellers :+ seller
  }

  def getStore: Store = {
    println(this)
    this
  }

  def onSellerCreated(seller: Seller): Store = {
    Store(sellers :+ seller)
  }

  def onGameUpdated(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int]): Store = {
    val seller = findSellerById(sellerId).get
    val newSeqWithRemovedElement = collectionOfSellers.filterNot(_.sellerId == sellerId)
    val newSeller = seller.copy(sellerId, replaceGame(Game(gameId, name.get, genre.get, price.get), seller.games))
    val newSellers = newSeqWithRemovedElement :+ newSeller
    this.copy(sellers = convert(newSellers))
  }

  def onGameRemoved(sellerId: String, gameId: String): Store = {
    val sellerFound = findSellerById(sellerId)
    val seller = sellerFound.get
    val game = findGameById(sellerId, gameId)
    val newGameListOfSeller = seller.games.filterNot(_.id == game.get.id)
    val newSeller = seller.copy(sellerId, newGameListOfSeller)
    val newSeqWithRemovedElement = collectionOfSellers.filterNot(_.sellerId == sellerId)
    val newSellers = newSeqWithRemovedElement :+ newSeller
    this.copy(convert(newSellers))
  }

  def onGameFoundById(sellerId: String, gameId: String): Store = {
    findGameById(sellerId, gameId) match {
      case Some(_) =>
        println(s"Game found with this id: $gameId")
        this.copy(sellers.filter(_.sellerId == sellerId))
      case None =>
        println(s"Game was not found with this id $gameId")
        this
    }
  }

  def onSellerFound(sellerId: String): Store = {
    findSellerById(sellerId) match {
      case Some(seller) =>
        println(s"Seller was found with this id: $sellerId")
        println(seller)
        Store(sellers.filter(_.sellerId == sellerId))
      case None =>
        println(s"Seller was not found with this id $sellerId")
        this
    }
  }

  def onGamesListed(sellerId: String): Store = {
    findSellerById(sellerId) match {
      case Some(_) =>
        println(s"Seller was found with this id: $sellerId")
        Store(sellers.filter(_.sellerId == sellerId))
      case None =>
        println(s"Seller was not found with this id $sellerId")
        this
    }

  }

  def onGameAdded(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int]): Store = {
    findSellerById(sellerId) match {
      case Some(seller) =>
        val newSeller = seller.copy(sellerId, seller.games :+ Game.optionalApply(gameId, name, genre, price).get)
        println(newSeller)
        val newSellers = toCollection(newSeller)
        this.copy(sellers = convert(newSellers))
      case None =>
        println(s"Seller was not found with this id $sellerId")
        this
    }
  }

  def applyCommand(cmd: Command): ReplyEffect[Event, Store] = cmd match {
    case AddGameToSeller(sellerId, gameId, name, genre, price, replyTo) => onAddGame(sellerId, gameId, name, genre, price, replyTo)
    case UpdateGameOfSeller(sellerId, gameId, name, genre, price, replyTo) => onUpdateGame(sellerId, gameId, name, genre, price, replyTo)
    case RemoveGameFromSeller(sellerId, gameId, replyTo) => onRemoveGame(sellerId, gameId, replyTo)
    case FindGameBySellerAndGameId(sellerId, gameId, replyTo) => onFindGameById(sellerId, gameId, replyTo)
    case FindSellerBySellerId(sellerId, replyTo) => onGetSeller(sellerId, replyTo)
    case CreateSeller(sellerId, replyTo) => onCreateSeller(sellerId, replyTo)
  }

  def applyEvent(evt: Event): Store = evt match {
    case GameAdded(sellerId, gameId, name, genre, price) => onGameAdded(sellerId, gameId, name, genre, price)
    case GameUpdated(sellerId, gameId, name, genre, price) => onGameUpdated(sellerId, gameId, name, genre, price)
    case GameRemoved(sellerId, gameId) => onGameRemoved(sellerId, gameId)
    case GameFound(sellerId, gameId) => onGameFoundById(sellerId, gameId)
    case SellerCreated(seller) => onSellerCreated(seller)
    case SellerFound(sellerId) => onSellerFound(sellerId)
  }

  def onAddGame(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int], replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {

    if (name.isEmpty) {
      Effect.reply(replyTo)(Rejected("Name cannot be empty"))
    } else if (genre.isEmpty) {
      Effect.reply(replyTo)(Rejected("Genre should be stated"))
    } else if (price.isEmpty) {
      Effect.reply(replyTo)(Rejected("Price cannot be zero"))
    }
    else {
      Effect
        .persist(GameAdded(sellerId, gameId, name, genre, price))
        .thenReply(replyTo) { addedSeller => Accepted(toSummary(addedSeller)) }
    }
  }

  private def onUpdateGame(sellerId: String, gameId: String, name: Option[String], genre: Option[String], price: Option[Int], replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {
    if (findGameById(sellerId, gameId).isDefined) {
      Effect
        .persist(GameUpdated(sellerId, gameId, name, genre, price))
        .thenReply(replyTo) { updatedSeller => Accepted(toSummary(updatedSeller)) }
    }
    else {
      Effect.reply(replyTo)(
        Rejected("Game doesn't exist")
      )
    }
  }

  private def onRemoveGame(sellerId: String, gameId: String, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {
    if (findGameById(sellerId, gameId).isEmpty) {
      Effect.reply(replyTo)(
        Rejected(
          s"Game with id: $gameId does not exist in seller"
        )
      )
    }
    else {
      Effect
        .persist(GameRemoved(sellerId, gameId))
        .thenReply(replyTo) {
          updatedSeller => Accepted(toSummary(updatedSeller))
        }
    }
  }

  private def onFindGameById(sellerId: String, gameId: String, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {
    if (findGameById(sellerId, gameId).isEmpty) {
      Effect.reply(replyTo)(
        Rejected(
          s"Game with id: $gameId does not exist in game seller")
      )
    } else {
      Effect
        .persist(GameFound(sellerId, gameId))
        .thenReply(replyTo) { updatedSeller => Accepted(toSummary(updatedSeller)) }
    }
  }

  private def onGetSeller(sellerId: String, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {
    if (findSellerById(sellerId).isDefined) {
      Effect
        .persist(SellerFound(sellerId))
        .thenReply(replyTo) {
          foundSeller => Accepted(toSummary(foundSeller))
        }
    } else Effect.reply(replyTo)(
      Rejected(
        s"Seller with id: $sellerId does not exist in store"
      ))
  }

  private def onCreateSeller(sellerId: String, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, Store] = {
    val newSeller = Seller(sellerId = sellerId, games = Seq.empty)
    Effect
      .persist(SellerCreated(newSeller))
      .thenReply(replyTo) { seller =>
        Accepted(toSummary(seller))
      }

  }
}

object Store {
  implicit val gameFormat: Format[Game] = Json.format
  implicit val gameListFormat: Format[GameList] = Json.format[GameList]
  implicit val gameSeqFormat: Format[GameSeq] = Json.format[GameSeq]
  implicit val sellerFormat: Format[Seller] = Json.format[Seller]
  implicit val SellersListFormat: Format[ListSellers] = Json.format[ListSellers]
  implicit val SellersSeqFormat: Format[SeqSellers] = Json.format[SeqSellers]

  implicit val format: Format[Store] = Json.format

  val empty: Store = Store(Seq.empty)

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, Store] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, Store](
        persistenceId = persistenceId,
        emptyState = Store.empty,
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Commands.Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))


  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Store")

}

