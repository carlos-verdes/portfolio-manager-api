
import sbt._

object Dependencies {

  object Versions {
    val ioFreeMonadsVersion = "0.1.2"
    val web3jVersion = "5.0.0"
    //val Http4sSpecs2Version = "1.0.0"
    val dockerTestV = "0.9.9"
  }

  object Libraries {
    val ioFreeMonads = "io.freemonads" %% "http4s-free" % Versions.ioFreeMonadsVersion
    val web3               = "org.web3j"          %  "core"                        % Versions.web3jVersion

    val dockerTestConfig   = "com.whisk"        %% "docker-testkit-config"       % Versions.dockerTestV
    val dockerTestSpecs2   = "com.whisk"        %% "docker-testkit-specs2"       % Versions.dockerTestV % "it, test"
    val dockerTestSpotify  = "com.whisk"        %% "docker-testkit-impl-spotify" % Versions.dockerTestV % "it, test"
    val javaxActivation    = "javax.activation" %  "activation"                  % "1.1.1" % "it"
    val javaxBind          = "javax.xml.bind"   %  "jaxb-api"                    % "2.3.0" % "it"
    val jaxbCore           = "com.sun.xml.bind" %  "jaxb-core"                   % "2.3.0" % "it"
    val jaxbImpl           = "com.sun.xml.bind" %  "jaxb-impl"                   % "2.3.0" % "it"
  }
}
