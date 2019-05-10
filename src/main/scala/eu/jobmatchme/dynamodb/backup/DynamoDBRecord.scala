package eu.jobmatchme.dynamodb.backup

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DynamoDBRecord(
  eventId: String,
  eventName: String,
  eventVersion: String,
  eventSource: String,
  awsRegion: String,
  eventSourceARN: String,
  data: DynamoDBRecordData
)

case class DynamoDBRecordData(
  approximateCreationDateTime: Date,
  sizeBytes: Long,
  sequenceNumber: String,
  streamViewType: String,
  keys: String,
  newImage: String
)

object DynamoDBRecord {
  implicit val dynamoDBRecordDatdReads: Reads[DynamoDBRecordData] = (
    (JsPath \ "approximateCreationDateTime").read[Date] and
      (JsPath \ "sizeBytes").read[Long] and
      (JsPath \ "sequenceNumber").read[String] and
      (JsPath \ "streamViewType").read[String] and
      (JsPath \ "keys").read[String] and
      (JsPath \ "newImage").read[String]
  )(DynamoDBRecordData.apply _)

  implicit val dynamoDBRecordReads: Reads[DynamoDBRecord] = (
    (JsPath \ "eventId").read[String] and
      (JsPath \ "eventName").read[String] and
      (JsPath \ "eventVersion").read[String] and
      (JsPath \ "eventSource").read[String] and
      (JsPath \ "awsRegion").read[String] and
      (JsPath \ "eventSourceARN").read[String] and
      (JsPath \ "dynamodb").read[DynamoDBRecordData]
  )(DynamoDBRecord.apply _)
}
