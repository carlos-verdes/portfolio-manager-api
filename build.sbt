
ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.callfolio"
ThisBuild / organizationName := "Call Folio"
ThisBuild / mainClass := Some("com.callfolio.portfolio.Main")

val Http4FreeVersion = "0.0.2"
val Web3jVersion = "5.0.0"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"))

lazy val root = (project in file("."))
  .settings(
    name := "portfolio-manager-api",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "io.freemonads" %% "http4s-free" % Http4FreeVersion,
      "org.web3j"     %  "core" % Web3jVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

addCommandAlias("sanity", ";clean ;compile ;scalastyle ;coverage ;test ;it:test ;coverageOff ;coverageReport ;project")

organizationName := "Call Folio"
startYear := Some(2021)
licenses += ("MIT", new URL("https://opensource.org/licenses/MIT"))
headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax
headerSettings(Test)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
