package eu.jobmatchme.dynamodb.backup

import akka.stream.alpakka.dynamodb.{ DynamoAttributes, DynamoClient, DynamoSettings }
import akka.stream.alpakka.dynamodb.scaladsl.DynamoDb
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.actor.ActorSystem
import akka.util.ByteString
import com.amazonaws.services.dynamodbv2.model.{ DescribeTableRequest, ScanRequest }
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.security.MessageDigest
import play.api.libs.json.Json
import scala.collection.JavaConverters._
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

object Backfill {
  def run(tableName: String, log: String => Unit)(implicit system: ActorSystem, mat: Materializer) {
    implicit val ec: ExecutionContext = system.dispatcher
    val config = ConfigFactory.load()
    val dynamodbAttributes = DynamoAttributes.client(DynamoClient(DynamoSettings(config.getConfig("dynamodb"))))
    val s3 = BucketConfig(config.getConfig("s3.incremental"))

    val describeSource = DynamoDb.source(new DescribeTableRequest().withTableName(tableName)).withAttributes(dynamodbAttributes).map(result => {
      val keyNames = result.getTable.getKeySchema.asScala.toSeq.map(element => element.getAttributeName)
      val scanSource = DynamoDb.source(new ScanRequest().withTableName(tableName)).withAttributes(dynamodbAttributes).map(scanResult => {
        scanResult.getItems.asScala.map(record => {
          val image = Json.stringify(DynamoDBUtil.mapToJson(record.asScala.toMap))
          val keys = Json.stringify(DynamoDBUtil.mapToJson(record.asScala.toMap.filter(entry => {keyNames.contains(entry._1)})))
          DynamoDBRecord("", "INSERT", "", "", "", "", DynamoDBRecordData(new Date, 0, "", "", keys, image))
        })
      }).map(recordBuffer => {
        recordBuffer.map(record => {
          val key = BigInt(1, MessageDigest.getInstance("MD5").digest(record.data.keys.getBytes)).toString(16)
          (key, ByteString(record.data.newImage))
        })
      })

      Await.result(
        scanSource.runWith(Sink.foreach(_.map(element => {
          Await.result(
            Source.single(element._2).runWith(S3.multipartUpload(s3.bucket, s3.prefix + element._1)).map(result => {
              log(s"Uploaded object ${result.getKey} to bucket ${result.getBucket} (Version: ${result.getVersionId})")
            }),
            10 seconds
          )
        }))),
        Duration.Inf
      )
    })

    Await.result(describeSource.runWith(Sink.ignore), Duration.Inf)
  }
}
