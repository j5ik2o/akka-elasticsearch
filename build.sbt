import Dependencies._
import Dependencies.Versions._

def crossScalacOptions(scalaVersion: String): Seq[String] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2L, scalaMajor)) if scalaMajor >= 12 =>
      Seq.empty
    case Some((2L, scalaMajor)) if scalaMajor <= 11 =>
      Seq("-Yinline-warnings")
  }

lazy val deploySettings = Seq(
  sonatypeProfileName := "com.github.j5ik2o",
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <url>https://github.com/j5ik2o/akka-kinesis</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:j5ik2o/akka-kinesis.git</url>
        <connection>scm:git:github.com/j5ik2o/akka-kinesis</connection>
        <developerConnection>scm:git:git@github.com:j5ik2o/akka-kinesis.git</developerConnection>
      </scm>
      <developers>
        <developer>
          <id>j5ik2o</id>
          <name>Junichi Kato</name>
        </developer>
      </developers>
  },
  publishTo := sonatypePublishToBundle.value,
  credentials := {
    val ivyCredentials = (baseDirectory in LocalRootProject).value / ".credentials"
    val gpgCredentials = (baseDirectory in LocalRootProject).value / ".gpgCredentials"
    Credentials(ivyCredentials) :: Credentials(gpgCredentials) :: Nil
  }
)

lazy val baseSettings = Seq(
  organization := "com.github.j5ik2o",
  scalaVersion := Versions.scala213Version,
  crossScalaVersions := Seq(Versions.scala212Version, Versions.scala213Version),
  scalacOptions ++= (Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:_",
      "-Ydelambdafy:method",
      "-target:jvm-1.8"
    ) ++ crossScalacOptions(scalaVersion.value)),
  resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      "es-snapshots" at "https://snapshots.elastic.co/maven/"
    ),
  parallelExecution in Test := false,
  scalafmtOnCompile in ThisBuild := true,
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
  .settings(baseSettings, dependenciesCommonSettings, deploySettings)
  .settings(
    name := "akka-elasticsearch-core",
    libraryDependencies ++= Seq(
        elasticsearch.highLevelClient
      ),
    parallelExecution in Test := false
  )

val `akka-elasticsearch-root` = (project in file("."))
  .settings(baseSettings, deploySettings)
  .settings(name := "akka-elasticsearch-root")
  .aggregate(`akka-elasticsearch-core`)
