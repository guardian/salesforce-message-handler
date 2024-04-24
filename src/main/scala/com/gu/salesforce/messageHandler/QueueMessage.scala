package com.gu.salesforce.messageHandler

import play.api.libs.json.{Format, Json}

case class QueueMessage(contactId: String)

object QueueMessage {
  implicit val messageFormat: Format[QueueMessage] = Json.format[QueueMessage]
}
