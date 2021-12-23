package com.sm.onlinegamestore.api

import play.api.libs.json.{Format, Json}

case class EventPublished(eventId:String, eventName:String)
object EventPublished {
  implicit val format:Format[EventPublished] = Json.format
}
