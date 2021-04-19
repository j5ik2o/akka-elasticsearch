import Dependencies._
import Dependencies.Versions._

def crossScalacOptions(scalaVersion: String): Seq[String] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2L, scalaMajor)) if scalaMajor >= 12 =>
      Seq.empty
    case Some((2L, scalaMajor)) if scalaMajor <= 11 =>
      Seq("-Yinline-warnings")
  }

lazy val baseSettings = Seq(
  organization := "com.github.j5ik2o",
  homepage := Some(url("https://github.com/j5ik2o/akka-elasticsearch")),
  licenses := List("The MIT License" -> url("http://opensource.org/licenses/MIT")),
  developers := List(
      Developer(
        id = "j5ik2o",
        name = "Junichi Kato",
        email = "j5ik2o@gmail.com",
        url = url("https://blog.j5ik2o.me")
      )
    ),
  scalaVersion := Versions.scala212Version,
  crossScalaVersions := Seq(Versions.scala212Version, Versions.scala213Version),
  scalacOptions ++= (Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:_",
      "-Ydelambdafy:method",
      "-target:jvm-1.8",
      "-Yrangepos",
      "-Ywarn-unused"
    ) ++ crossScalacOptions(scalaVersion.value)),
  resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      "es-snapshots" at "https://snapshots.elastic.co/maven/"
    ),
  ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  Test / publishArtifact := false,
  Test / parallelExecution := false,
  envVars := Map(
      "AWS_REGION" -> "ap-northeast-1"
    )
)

val dependenciesCommonSettings = Seq(
  libraryDependencies ++= Seq(
      typesafe.akka.slf4j,
      typesafe.akka.stream,
      apache.log4jToSlf4j,
      slf4j.api,
      testcontainers.testcontainers              % Test,
      testcontainers.testcontainersElasticsearch % Test,
      scalatest.scalatest                        % Test,
      logback.classic                            % Test,
      typesafe.akka.testkit                      % Test,
      typesafe.akka.streamTestkit                % Test
    ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2L, scalaMajor)) if scalaMajor <= 12 =>
        Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.4.2")
      case _ =>
        Seq.empty
    }
  },
  Test / fork := true
)

val `akka-elasticsearch-core` = (project in file("akka-elasticsearch-core"))
  .settings(baseSettings, dependenciesCommonSettings)
  .settings(
    name := "akka-elasticsearch-core",
    libraryDependencies ++= Seq(
        elasticsearch.highLevelClient
      ),
    parallelExecution in Test := false
  )

val `akka-elasticsearch-root` = (project in file("."))
  .settings(baseSettings)
  .settings(name := "akka-elasticsearch-root")
  .aggregate(`akka-elasticsearch-core`)

// --- Custom commands
addCommandAlias("lint", ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck;scalafixAll --check")
addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
