package eu.jobmatchme.dynamodb.backup

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.dynamodb.{ AwsOp, DynamoAttributes, DynamoClient, DynamoSettings }
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.dynamodb.scaladsl.DynamoDb
import akka.stream.scaladsl.{ Framing, Source, Sink }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.ByteString
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{ Json, JsObject }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Restore {
  def run(key: String, tableName: String, log: String => Unit)(implicit system: ActorSystem, mat: Materializer) {
    val config = ConfigFactory.load()
    val dynamodbAttributes = DynamoAttributes.client(DynamoClient(DynamoSettings(config.getConfig("dynamodb"))))
    val s3 = BucketConfig(config.getConfig("s3.snapshot"))

    val stream = S3.download(s3.bucket, s3.prefix + key)
      .flatMapConcat(_.get._1)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 10485760 /* aka 10 mb */, allowTruncation = false))
      .map(_.utf8String)
      .map(result => {
        AwsOp.create(new PutItemRequest(tableName, DynamoDBUtil.toAttribute(Json.parse(result).as[JsObject])))
      }).via(DynamoDb.flow).withAttributes(dynamodbAttributes).map(
        result => log(".")
      )

    Await.result(stream.runWith(Sink.ignore), Duration.Inf)
  }
}
