package eu.jobmatchme.dynamodb.backup

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{ Source, Sink }
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import java.util.Date
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.Await

object Snapshot {
  def run(log: String => Unit)(implicit system: ActorSystem, mat: Materializer) {
    implicit val ec: ExecutionContext = system.dispatcher
    val key = "%1$tY-%1$tm-%1$td %1$tH:%1$tM".format(new Date())
    val config = ConfigFactory.load()
    val from = BucketConfig(config.getConfig("s3.incremental"))
    val to = BucketConfig(config.getConfig("s3.snapshot"))

    val source: Source[ByteString, NotUsed] = S3.listBucket(from.bucket, Some(from.prefix)).flatMapConcat(result => {
      S3.download(from.bucket, result.key).flatMapConcat(
        x => x.map(_._1.concat(Source.single[ByteString](ByteString("\n")))).getOrElse(Source.empty)
      )
    })

    Await.result(
      source.runWith(
        S3.multipartUpload(to.bucket, to.prefix + key).mapMaterializedValue(_.map(result => {
          log(s"Uploaded object ${result.getKey} to bucket ${result.getBucket} (Version: ${result.getVersionId})")
        }))
      ), Duration.Inf
    )
  }
}
