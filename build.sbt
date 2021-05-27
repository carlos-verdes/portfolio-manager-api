
ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.callfolio"
ThisBuild / organizationName := "Call Folio"

lazy val root = (project in file("."))
  .settings(
    name := "portfolio-manager-api",
    libraryDependencies ++= Seq(
      "io.freemonads" %% "http4s-free" % Http4FreeVersion
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
