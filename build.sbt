name := """dynamodb-backup"""
organization := """jobmatch.me"""

version := "0.0.1-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.8"

scalacOptions ++= Seq("-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

mainClass in assembly := Some("eu.jobmatchme.dynamodb.backup.Cli")
assemblyJarName in assembly := "dynamodb-backup.jar"

libraryDependencies += "com.typesafe" % "config" % "1.3.4"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.0.0"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-dynamodb" % "1.0.0"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.2"
libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"
libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "2.2.6"
