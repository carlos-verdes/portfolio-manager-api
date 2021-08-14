import Dependencies._
import Libraries._

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.callfolio"
ThisBuild / organizationName := "Call Folio"
ThisBuild / mainClass := Some("com.callfolio.portfolio.Main")
ThisBuild / fork := true
ThisBuild / cancelable := true


val Http4FreeVersion = "0.0.9"
val Http4sSpecs2Version = "1.0.0"
val Web3jVersion = "5.0.0"
val DockerTestVersion = "0.9.9"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/public")

val mainLibraries = Seq(ioFreeMonads)
val testLibraries = Seq(dockerTestConfig, dockerTestSpecs2, dockerTestSpotify)
val javaxLibraries = Seq(javaxBind, javaxActivation, jaxbCore, jaxbImpl)

val allLib = mainLibraries ++ testLibraries ++ javaxLibraries

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "dfolio-api",
    Defaults.itSettings,
    publishMavenStyle := true,
    Compile / herokuAppName := "dfolio-api",
    libraryDependencies ++= allLib,
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

addCommandAlias("sanity", ";clean ;compile ;scalastyle ;coverage ;test ;it:test ;coverageOff ;coverageReport ;project")

coverageExcludedPackages := """com.callfolio.portfolio.Main"""

startYear := Some(2021)
licenses += ("MIT", new URL("https://opensource.org/licenses/MIT"))
headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax
headerSettings(Test)

enablePlugins(JavaAppPackaging)

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
