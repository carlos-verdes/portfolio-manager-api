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

  override def run(args: List[String]): IO[ExitCode] = {

    println(
      """  __     ___       ___
        | /\ \  /'___\     /\_ \    __
        | \_\ \/\ \__/  ___\//\ \  /\_\    ___
        | /'_` \ \ ,__\/ __`\\ \ \ \/\ \  / __`\
        |/\ \L\ \ \ \_/\ \L\ \\_\ \_\ \ \/\ \L\ \
        |\ \___,_\ \_\\ \____//\____\\ \_\ \____/
        | \/__,_ /\/_/ \/___/ \/____/ \/_/\/___/
        |""".stripMargin)

    println(s"Starting server... $serverConfig")
    println(s"Arango config... $arangoConfig")

    BlazeServerBuilder[IO](executionContext)
        .bindHttp(serverConfig.port, serverConfig.host)
        .withHttpApp(app)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
  }
}
