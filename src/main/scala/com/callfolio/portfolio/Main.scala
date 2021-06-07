/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio

import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2.Arango
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.data.EitherK
import cats.effect.{ExitCode, IO, IOApp}
import cats.~>
import io.freemonads.arango._
import io.freemonads.crypto.CryptoAlgebra
import io.freemonads.crypto.interpreters._
import io.freemonads.http.resource.ResourceAlgebra
import io.freemonads.http.rest.{Http4sAlgebra, http4sInterpreter}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  import User._

  type PortfolioAlgebraA[R] = EitherK[Http4sAlgebra, ResourceAlgebra, R]
  type PortfolioAlgebra[R] = EitherK[CryptoAlgebra, PortfolioAlgebraA, R]

  val arangoConfig = ArangoConfiguration.load()
  val arangoResource = Arango(arangoConfig)
  val serverConfig = ServerConfig.load()

  implicit def unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  implicit def interpreters: PortfolioAlgebra ~> IO = {
    ethereumCryptoInterpreter[IO] or
    (http4sInterpreter[IO] or arangoResourceInterpreter(arangoResource))
  }

  val app = Router("/users" -> userRoutes[PortfolioAlgebra, VPackEncoder, VPackDecoder]).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](executionContext)
        .bindHttp(serverConfig.port, serverConfig.host)
        .withHttpApp(app)
        .resource
        .use(_ => IO.never)
        .start
        .as(ExitCode.Success)
}
