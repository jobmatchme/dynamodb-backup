package eu.jobmatchme.dynamodb.backup

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{ APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent, DynamodbEvent }
import play.api.libs.json._
import scala.util.Try
import scala.collection.JavaConverters._

class Lambda {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  def backup(event: DynamodbEvent, context: Context): APIGatewayProxyResponseEvent = {
    val logger = context.getLogger
    val records = event.getRecords.asScala.map(record => {
      val keys = DynamoDBUtil.mapToJson(record.getDynamodb.getKeys.asScala.toMap)
      val newImage = if (record.getEventName != "REMOVE") DynamoDBUtil.mapToJson(record.getDynamodb.getNewImage.asScala.toMap) else null
      DynamoDBRecord(
        record.getEventID,
        record.getEventName,
        record.getEventVersion,
        record.getEventSource,
        record.getAwsRegion,
        record.getEventSourceARN,
        DynamoDBRecordData(
          record.getDynamodb.getApproximateCreationDateTime,
          record.getDynamodb.getSizeBytes,
          record.getDynamodb.getSequenceNumber,
          record.getDynamodb.getStreamViewType,
          Json.stringify(keys),
          Json.stringify(newImage)
        )
      )
    })
    Try({
      Backup.run(records, logger.log)
      respondOk
    }) recover {
      case error => {
        logger.log(StackTraceUtil.getStackTrace(error))
        respondInternalServerError
      }
    } get
  }

  def restore(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val logger = context.getLogger
    val json = Json.parse(event.getBody)
    val maybeTableName = (json \ "tableName").get
    val maybeKey = (json \ "snapshotFilename").get
    maybeTableName.asOpt[String].map(tableName => {
      maybeKey.asOpt[String].map(key => {
        Try({
          Restore.run(key, tableName, logger.log)
          respondOk
        }) recover {
          case error => {
            logger.log(StackTraceUtil.getStackTrace(error))
            respondInternalServerError
          }
        } get
      }).getOrElse(respondBadRequest)
    }).getOrElse(respondBadRequest)
  }

  def snapshot(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val logger = context.getLogger
    Try({
      Snapshot.run(logger.log)
      respondOk
    }) recover {
      case error => {
        logger.log(StackTraceUtil.getStackTrace(error))
        respondInternalServerError
      }
    } get
  }

  private def respondOk: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent().withStatusCode(200)
  private def respondBadRequest: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent().withStatusCode(400)
  private def respondInternalServerError: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent().withStatusCode(500)
}
