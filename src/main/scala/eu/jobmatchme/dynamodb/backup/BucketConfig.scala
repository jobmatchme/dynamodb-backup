package eu.jobmatchme.dynamodb.backup

import com.typesafe.config.Config

case class BucketConfig(bucket: String, prefix: String)

object BucketConfig {
  def apply(config: Config): BucketConfig = {
    val separator = if (config.getString("prefix").endsWith("/")) "" else "/"
    BucketConfig(config.getString("bucket"), config.getString("prefix") + separator)
  }
}
