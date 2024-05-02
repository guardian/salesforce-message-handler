package com.gu.salesforce.messageHandler

import com.amazonaws.util.IOUtils
import salesforce.soap.Notifications
import org.specs2.mutable.Specification

class SOAPNotificationsParserTest extends Specification {
  def getTestString(fileName: String) = IOUtils.toString(getClass.getResourceAsStream(s"/$fileName"))

  "Parser" should {
    "parse a SOAP message" in {
      val notifications: Notifications =
        SOAPNotificationsParser.parseMessage(getTestString("soapNotificationsRequest.xml"))

      notifications.Notification.head.sObject.Id must beSome("aContactId")
    }
  }
}
