package eu.jobmatchme.dynamodb.backup

import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.actor.ActorSystem
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import java.security.MessageDigest
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

object Backup {
  def run(records: Seq[DynamoDBRecord], log: String => Unit)(implicit system: ActorSystem, mat: Materializer) {
    implicit val ec: ExecutionContext = system.dispatcher
    val config = ConfigFactory.load()
    val s3 = BucketConfig(config.getConfig("s3.incremental"))

    records.foreach(record => {
      val key = BigInt(1, MessageDigest.getInstance("MD5").digest(record.data.keys.getBytes)).toString(16)

      if (record.eventName != "REMOVE")
        Await.result(
          Source.single(
            ByteString(record.data.newImage)
          ).runWith(
            S3.multipartUpload(s3.bucket, s3.prefix + key)
          ).map(
            result => log(s"Updated object ${result.getKey} in bucket ${result.getBucket} (Version: ${result.getVersionId})")
          ), 10 seconds
        )
      else
        Await.result(S3.deleteObject(s3.bucket, s3.prefix + key).runWith(Sink.ignore).map(
          result => log(s"Updated object ${s3.prefix + key} in bucket ${s3.bucket} (Delete Marker)")
        ), 10 seconds)
    })
  }
}
