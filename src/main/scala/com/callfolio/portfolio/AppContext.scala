/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2.Arango
import cats.data.EitherK
import cats.effect.{Concurrent, ContextShift, IO, Resource, Sync, Timer}
import cats.~>
import io.freemonads.arango.arangoResourceInterpreter
import io.freemonads.crypto.CryptoAlgebra
import io.freemonads.crypto.interpreters.ethereumCryptoInterpreter
import io.freemonads.http.resource.ResourceAlgebra
import io.freemonads.http.rest.{Http4sAlgebra, http4sInterpreter}
import io.freemonads.security.SecurityAlgebra
import io.freemonads.security.jwt.jwtSecurityInterpreter
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait AppContext {

  type PortfolioAlgebraA[R] = EitherK[Http4sAlgebra, ResourceAlgebra, R]
  type PortfolioAlgebraB[R] = EitherK[CryptoAlgebra, PortfolioAlgebraA, R]
  type PortfolioAlgebra[R] = EitherK[SecurityAlgebra, PortfolioAlgebraB, R]

  val arangoConfig = ArangoConfiguration.load()
  def arangoResource[F[_]: ContextShift : Timer : Concurrent : Logger]: Resource[F, Arango[F]] = Arango[F](arangoConfig)
  val serverConfig = ServerConfig.load()

  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  implicit def interpreters(implicit CS: ContextShift[IO], T: Timer[IO]): PortfolioAlgebra ~> IO = {
    jwtSecurityInterpreter[IO] or
      (ethereumCryptoInterpreter[IO] or
        (http4sInterpreter[IO] or arangoResourceInterpreter(arangoResource[IO])))
  }
}
