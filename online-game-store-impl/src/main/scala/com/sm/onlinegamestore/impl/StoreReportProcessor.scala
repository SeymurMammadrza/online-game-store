package com.sm.onlinegamestore.impl

import akka.{Done, NotUsed}
import akka.persistence.query.Offset
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor.ReadSideHandler

import scala.concurrent.{ExecutionContext, Future}

class StoreReportProcessor()(implicit ec: ExecutionContext)
  extends ReadSideProcessor[Events.Event]{

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Events.Event] = {
    new ReadSideHandler[Events.Event] {
      override def globalPrepare(): Future[Done] =
        StoreReportRepository.createTables()

      override def prepare(tag: AggregateEventTag[Events.Event]): Future[Offset] =
        StoreReportRepository.loadOffset(tag)

      override def handle(): Flow[EventStreamElement[Events.Event], Done, NotUsed] =
        Flow[EventStreamElement[Events.Event]]
          .mapAsync(1) { eventElement =>
            StoreReportRepository.handleEvent(eventElement.event, eventElement.offset)
          }
    }
  }
  override def aggregateTags: Set[AggregateEventTag[Events.Event]] =
    Set(Events.Event.Tag)
}
