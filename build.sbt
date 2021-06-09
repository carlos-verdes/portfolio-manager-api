
ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.callfolio"
ThisBuild / organizationName := "Call Folio"
ThisBuild / mainClass := Some("com.callfolio.portfolio.Main")


val Http4FreeVersion = "0.0.2"
val Web3jVersion = "5.0.0"
val DockerTestVersion = "0.9.9"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"))

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "dfolio-api",
    Defaults.itSettings,
    publishMavenStyle := true,
    Compile / herokuAppName := "dfolio-api",
    libraryDependencies ++= Seq(
      "io.freemonads" %% "http4s-free" % Http4FreeVersion,
      "org.web3j"     %  "core" % Web3jVersion,
      "com.whisk"        %% "docker-testkit-specs2"       % DockerTestVersion,
      "com.whisk"        %% "docker-testkit-impl-spotify" % DockerTestVersion % "it, test",
      "javax.activation" %  "activation"                  % "1.1.1" % "it, test",
      "javax.xml.bind"   %  "jaxb-api"                    % "2.3.0" % "it, test",
      "com.sun.xml.bind" %  "jaxb-core"                   % "2.3.0" % "it, test",
      "com.sun.xml.bind" %  "jaxb-impl"                   % "2.3.0" % "it, test"
    ),
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

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

