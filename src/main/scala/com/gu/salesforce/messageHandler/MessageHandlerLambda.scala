package com.gu.salesforce.messageHandler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.sqs.model.SendMessageResult
import com.gu.salesforce.messageHandler.APIGatewayResponse._
import com.gu.salesforce.messageHandler.SOAPNotificationsParser.parseMessage
import salesforce.soap.ContactNotification
import play.api.libs.json.{JsValue, Json}

import java.io.{InputStream, OutputStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

trait RealDependencies {
  val queueClient = SqsClient
}

trait MessageHandler extends Logging {
  def queueClient: QueueClient

  val queueName = s"salesforce-outbound-messages-${Config.stage}"

  def credentialsAreValid(inputEvent: JsValue): Boolean = {

    val maybeApiClientId = (inputEvent \ "queryStringParameters" \ "apiClientId").asOpt[String]
    val maybeApiClientToken = (inputEvent \ "queryStringParameters" \ "apiToken").asOpt[String]
    val maybeCredentials = (maybeApiClientId, maybeApiClientToken)
    maybeCredentials match {
      case (Some(apiClientId), Some(apiToken)) => {
        (apiClientId == Config.apiClientId && apiToken == Config.apiToken)
      }
      case _ => {
        logger.info(s"Could not find credentials in request")
        false
      }
    }
  }

  def sendToQueue(notification: ContactNotification): Future[Try[SendMessageResult]] = {
    val queueMessage = QueueMessage(notification.sObject.Id.get)
    val queueMessageString = Json.prettyPrint(Json.toJson(queueMessage))
    queueClient.send(queueName, queueMessageString)
  }

  def processNotifications(notifications: Seq[ContactNotification], outputStream: OutputStream) = {
    val contactListStr = notifications.flatMap(_.sObject.Id).mkString(", ")
    logger.info(s"contacts found in salesforce xml: [$contactListStr]")
    val FutureResponses = notifications.map(sendToQueue)
    val future = Future.sequence(FutureResponses).map { responses =>
      val errors = responses collect { case Failure(error) => error }
      if (errors.nonEmpty) {
        errors.foreach(error => logger.error(s"error while trying to send message to queue", error))
        logger.info(s"lambda execution failed. Contacts in request: [$contactListStr]")
        outputForAPIGateway(outputStream, internalServerError)
      } else {
        logger.info(s"lambda execution successful. Enqueued contacts: [$contactListStr]")
        outputForAPIGateway(outputStream, okResponse)
      }
    }
    Await.ready(future, Duration.Inf)
  }

  def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {

    logger.info(s"Salesforce message handler lambda ${Config.stage} is starting up...")
    logger.info(s"using config from ${Config.bucket}/${Config.key}")
    val inputEvent = Json.parse(inputStream)
    if (!credentialsAreValid(inputEvent)) {
      logger.info("Request could not be authenticated")
      outputForAPIGateway(outputStream, unauthorized)
    } else {
      logger.info("Authenticated request successfully...")
      val body = (inputEvent \ "body").as[String]
      val parsedMessage = parseMessage(body)
      if (parsedMessage.OrganizationId.startsWith(Config.salesforceOrganizationId)) {
        processNotifications(parsedMessage.Notification, outputStream)
      } else {
        logger.info("Unexpected salesforce organization id in xml message")
        outputForAPIGateway(outputStream, unauthorized)
      }
    }
  }
}

object Lambda extends MessageHandler with RealDependencies