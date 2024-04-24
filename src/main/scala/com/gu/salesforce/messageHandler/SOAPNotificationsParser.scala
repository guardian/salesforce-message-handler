package com.gu.salesforce.messageHandler

import salesforce.soap.Notifications
import scalaxb._
import soapenvelope11.Envelope

import scala.xml.{Elem, XML}

object SOAPNotificationsParser {

  /**
   * Given a 'notifications' request from Salesforce (which comes embedded in a SOAP Envelope), extract the
   * 'Notifications' object.
   *
   * https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/sforce_api_om_outboundmessaging_wsdl.htm
   */
  def parseMessage(soapRequestEnvelope: String): Notifications = {
    val envelope: Envelope = fromXML[soapenvelope11.Envelope](XML.loadString(soapRequestEnvelope))

    val notificationsXml = envelope.Body.any.collect({
      case DataRecord(_, _, x: Elem) if x.label != "Fault" => x
    }).head

    fromXML[Notifications](notificationsXml)
  }
}
