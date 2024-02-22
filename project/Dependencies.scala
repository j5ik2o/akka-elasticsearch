import sbt._

object Dependencies {

  object Versions {
    val scala212Version            = "2.12.13"
    val scala213Version            = "2.13.5"
    val akkaVersion                = "2.6.13"
    val testcontainersScalaVersion = "0.39.3"
    val scalaTestVersion           = "3.2.6"
    val logbackVersion             = "1.2.5"
  }

  object typesafe {

    object akka {
      val slf4j         = "com.typesafe.akka" %% "akka-slf4j"          % Versions.akkaVersion
      val stream        = "com.typesafe.akka" %% "akka-stream"         % Versions.akkaVersion
      val testkit       = "com.typesafe.akka" %% "akka-testkit"        % Versions.akkaVersion
      val streamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akkaVersion

    }
  }

  object apache {
    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.23.0"
  }

  object slf4j {
    val api = "org.slf4j" % "slf4j-api" % "1.7.32"

  }

  object testcontainers {
    val testcontainers              = "org.testcontainers" % "testcontainers" % "1.19.6"
    val testcontainersElasticsearch = "org.testcontainers" % "elasticsearch"  % "1.19.6"

  }

  object scalatest {
    val scalatest = "org.scalatest" %% "scalatest" % Versions.scalaTestVersion
  }

  object logback {
    val classic = "ch.qos.logback" % "logback-classic" % Versions.logbackVersion
  }

  object elasticsearch {
    val highLevelClient = "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.18"
  }

}
