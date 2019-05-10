package eu.jobmatchme.dynamodb.backup

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import scala.util.Try

object Cli extends App {
  def printUsageAndExit() = {
    println(
      "CLI usage:\n" +
      "--snapshot                         Create a new snapshot of the current state of the incremental backup\n" +
      "--restore <tableName> <snapshot>   Restore a snapshot to a table\n" +
      "--backfill <tableName>             Create backup objects from an existing table"
    )
    sys.exit(1)
  }

  if (args.length == 0) printUsageAndExit()

  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  val config = ConfigFactory.load()

  args(0) match {
    case "--snapshot" =>
      Try({
        println("Starting, this could take a while...")
        Snapshot.run(s => println(s))
        println("Snapshot successfully created")
        sys.exit()
      }) recover {
        case error => {
          println("An error occurred:")
          error.printStackTrace()
          sys.exit(1)
        }
      }
    case "--restore"  =>
      if (args.length != 3) printUsageAndExit()

      Try({
        println("Starting, this could take a while...")
        Restore.run(args(2), args(1), s => println(s))
        println("Successfully applied backup")
        sys.exit()
      }) recover {
        case error => {
          println("An error occurred:")
          error.printStackTrace()
          sys.exit(1)
        }
      }
    case "--backfill" =>
      if (args.length != 2) printUsageAndExit()

      Try({
        println("Starting, this could take a while...")
        Backfill.run(args(1), s => println(s))
        println("Successfully backfilled bucket with backup data")
        sys.exit()
      }) recover {
        case error => {
          println("An error occurred:")
          error.printStackTrace()
          sys.exit(1)
        }
      }
    case _ => printUsageAndExit()
  }
}
