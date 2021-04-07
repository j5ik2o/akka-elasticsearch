val scala212Version = "2.12.13"
val scala213Version = "2.13.5"

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
  scalaVersion := scala213Version,
  crossScalaVersions := Seq(scala212Version, scala213Version),
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

val akkaVersion                = "2.6.14"
val testcontainersScalaVersion = "0.39.3"
val scalaTestVersion           = "3.2.6"
val logbackVersion             = "1.2.3"

val dependenciesCommonSettings = Seq(
  libraryDependencies ++= Seq(
      "com.typesafe.akka"       %% "akka-slf4j"          % akkaVersion,
      "com.typesafe.akka"       %% "akka-stream"         % akkaVersion,
      "org.apache.logging.log4j" % "log4j-to-slf4j"      % "2.11.1",
      "org.slf4j"                % "slf4j-api"           % "1.7.30",
      "org.testcontainers"       % "testcontainers"      % "1.15.2"         % Test,
      "org.testcontainers"       % "elasticsearch"       % "1.15.2"         % Test,
      "org.scalatest"           %% "scalatest"           % scalaTestVersion % Test,
      "ch.qos.logback"           % "logback-classic"     % logbackVersion   % Test,
      "com.typesafe.akka"       %% "akka-testkit"        % akkaVersion      % Test,
      "com.typesafe.akka"       %% "akka-stream-testkit" % akkaVersion      % Test
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
        "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.12.0"
      ),
    parallelExecution in Test := false
  )

val `akka-elasticsearch-root` = (project in file("."))
  .settings(baseSettings, deploySettings)
  .settings(name := "akka-elasticsearch-root")
  .aggregate(`akka-elasticsearch-core`)
