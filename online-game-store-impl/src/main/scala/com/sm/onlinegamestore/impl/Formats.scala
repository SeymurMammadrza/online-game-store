package com.sm.onlinegamestore.impl

import com.sm.onlinegamestore.impl.Commands.{Accepted, Rejected, Summary}
import play.api.libs.json.{Format, Json}

object Formats {

  implicit val summaryFormat: Format[Summary] = Json.format
  implicit val confirmationAccepted: Format[Accepted] = Json.format
  implicit val confirmationRejectedFormat: Format[Rejected] = Json.format
}
