import com.lightbend.lagom.core.LagomVersion.{current => lagomVersion}

ThisBuild / organization := "com.sm"
ThisBuild / version := "0.1"

// the Scala version that will be used for cross-compiled libraries
ThisBuild / version := "2.13.0"

ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

val macwire = "com.softwaremill.macwire" %% "macros" % "2.5.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9" % Test
val logback = "ch.qos.logback" % "logback-classic" % "1.2.7" % Runtime
val lagomScaladslAkkaDiscovery = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % lagomVersion
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.1.1"


ThisBuild / lagomKafkaEnabled := false
ThisBuild / lagomKafkaPort := 9092
ThisBuild / lagomCassandraEnabled := false
ThisBuild / lagomCassandraPort := 9042

lazy val `online-game-store` = (project in file("."))
  .aggregate(`online-game-store-api`, `online-game-store-impl`)

lazy val `online-game-store-api` = (project in file("online-games-store-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      logback
    )
  )

lazy val `online-game-store-impl` = (project in file("online-games-store-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      macwire,
      logback,
      scalaTest
    )
  ).settings(lagomForkedTestSettings)
  .dependsOn(`online-game-store-api`)



