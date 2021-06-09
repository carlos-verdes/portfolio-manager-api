/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp with AppContext {

  import User._

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
