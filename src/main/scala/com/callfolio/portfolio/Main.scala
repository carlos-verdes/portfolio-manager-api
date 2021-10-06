/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio


import java.security.{NoSuchAlgorithmException, SecureRandom, Security}

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.toSemigroupKOps
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.middleware.{CORS, Logger}


object Main extends AppContext with IOApp {

  import User._

  // Windows testing hack
  private def tsecWindowsFix(): Unit =
    try {
      SecureRandom.getInstance("NativePRNGNonBlocking")
      ()
    } catch {
      case _: NoSuchAlgorithmException =>
        val secureRandom = new SecureRandom()
        val defaultSecureRandomProvider = secureRandom.getProvider.get(s"SecureRandom.${secureRandom.getAlgorithm}")
        secureRandom.getProvider.put("SecureRandom.NativePRNGNonBlocking", defaultSecureRandomProvider)
        Security.addProvider(secureRandom.getProvider)
        ()
    }

  tsecWindowsFix()

  val authMiddlewareInstance = authMiddleware
  val routes = publicUserRoutes <+> authMiddlewareInstance(privateUserRoutes)
  val app = routes.orNotFound

  val corsApp = CORS(Logger.httpApp(true, true)(app))

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
        .withHttpApp(corsApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
  }
}
