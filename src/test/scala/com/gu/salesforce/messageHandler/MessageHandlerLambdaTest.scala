package com.gu.salesforce.messageHandler

import com.amazonaws.services.sqs.model.SendMessageResult
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import java.io.ByteArrayOutputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Success, Try }

case class TestLambda(queueClient: QueueClient) extends MessageHandler

class MessageHandlerLambdaTest extends Specification with Mockito {
  def getTestInputStream(fileName: String) = {
    getClass.getResourceAsStream(s"/$fileName")
  }

  "Handler" should {

    val successResponseBody =
      """<?xml version="1.0" encoding="UTF-8"?>
            |<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
            |	<soapenv:Body>
            |		<notificationsResponse xmlns="http://soap.sforce.com/2005/09/outbound">
            |			<Ack>true</Ack>
            |		</notificationsResponse>
            |	</soapenv:Body>
            |</soapenv:Envelope>
            |    """.stripMargin

    val successApiResponse = Json.obj(
      "statusCode" -> "200",
      "headers" -> Json.obj(
        "Content-Type" -> "text/xml"),
      "body" -> successResponseBody)

    "handle single contact requests" in {

      val mockSuccess = mock[SendMessageResult]
      val successResponse: Future[Try[SendMessageResult]] = Future.successful(Success(mockSuccess))
      val mockSqsClient = mock[QueueClient]
      mockSqsClient.send(anyString, anyString)(any[ExecutionContext]) returns successResponse

      val lambda = TestLambda(mockSqsClient)
      val outputStream = new ByteArrayOutputStream
      lambda.handleRequest(getTestInputStream("simpleUpdate.json"), outputStream, null)

      Json.parse(outputStream.toString) shouldEqual successApiResponse
      val expectedQueuedMessage = Json.prettyPrint(Json.obj("contactId" -> "aContactId"))
      there was one(mockSqsClient).send("salesforce-outbound-messages-DEV", expectedQueuedMessage)
    }

  }
}
