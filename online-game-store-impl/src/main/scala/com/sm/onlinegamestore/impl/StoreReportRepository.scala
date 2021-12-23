package com.sm.onlinegamestore.impl

import akka.Done
import akka.persistence.query.{NoOffset, Offset}
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import Events._
import com.sm.onlinegamestore.api
import com.sm.onlinegamestore.api.{Game, Seller, View}

import java.util.NoSuchElementException
import scala.collection.immutable.Seq
import scala.concurrent.Future

object StoreReportRepository {

  var collectionForReport: Seq[Seller] = Seq()

  def replace[A](element: A, seq: scala.Seq[A]): scala.Seq[A] = seq.map(x => if (x.equals(element)) element else x)

  def getSeller(sellerId: String): Option[Seller] =
    collectionForReport.find(_.sellerId == sellerId)


  def replaceSeller(seller: Seller, seq: Seq[Seller]): Seq[Seller] = {
    seq.find(sellerSearched => sellerSearched.sellerId == seller.sellerId) match {
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
    seq.find(gameSearched => gameSearched.id == game.id) match {
      case Some(value) =>
        val newSeq = seq.filterNot(_.id == game.id)
        val newSeller = value.copy(game.id, game.name,game.genre,game.price)
        val updatedSeq = newSeq :+ newSeller
        print(s"updatedSeq: $updatedSeq")
        updatedSeq
      case None => seq
    }
  }

  def createTables(): Future[Done] =
    Future.successful(Done)

  def loadOffset(tag: AggregateEventTag[Events.Event]): Future[Offset] =
    Future.successful(NoOffset)

  def handleEvent(event: Events.Event, offset: Offset): Future[Done] = {
    event match {
      case SellerCreated(seller) =>
        collectionForReport = collectionForReport :+ seller
        View(collectionForReport)
      case GameAdded(sellerId, gameId, name, genre, price) =>
        getSeller(sellerId) match {
          case Some(seller) =>
            seller.games :+ Game.optionalApply(gameId, name, genre, price)
            collectionForReport = collectionForReport :+ seller
            api.View(collectionForReport)
          case None =>
            new NoSuchElementException("seller was not found")
        }
      case GameRemoved(sellerId, gameId) =>
        getSeller(sellerId) match {
          case Some(seller) => seller.games.filterNot(game => game.id == gameId)
            val newSeq = replaceSeller(seller, collectionForReport)
            api.View(newSeq)
          case None =>
            new NoSuchElementException("seller was not found")
        }
      case GameUpdated(sellerId, gameId, name, genre, price) =>
        getSeller(sellerId) match {
          case Some(seller) =>
            seller.games.find(game => game.id == gameId) match {
              case Some(game) => Game(gameId, name.get, genre.get, price.get)
                val newGames = replaceGame(game, seller.games)
                val newSeller = seller.copy(sellerId,newGames)
                val newSeq = replaceSeller(newSeller, collectionForReport)
                api.View(newSeq)
              case None => new NoSuchElementException("game was not found")
            }
          case None => new NoSuchElementException("seller was not found")
        }
      case GameFound(sellerId, gameId) => getSeller(sellerId) match {
        case Some(seller) =>
          seller.games.find(game => game.id == gameId) match {
            case Some(game) => println(game)
            case None => new NoSuchElementException("game was not found")
          }
        case None => new NoSuchElementException("seller was not found")
      }
      case SellerFound(sellerId) => getSeller(sellerId) match {
        case Some(seller) => println(seller)
        case None => new NoSuchElementException("seller was not found")
      }
    }
    Future.successful(Done)
  }
}

