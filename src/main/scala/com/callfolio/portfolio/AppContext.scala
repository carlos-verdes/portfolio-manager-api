/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2.Arango
import cats.effect._
import io.freemonads.tagless.crypto.CryptoAlgebra
import io.freemonads.tagless.crypto.ethereum.EthereumCryptoInterpreter
import io.freemonads.tagless.http.HttpAlgebra
import io.freemonads.tagless.http.io.ioHttpAlgebraInterpreter
import io.freemonads.tagless.interpreters.arangoStore.{ArangoStoreAlgebra, ArangoStoreInterpreter}
import io.freemonads.tagless.security.SecurityAlgebra
import io.freemonads.tagless.security.jwt.ioJwtSecurityInterpreter
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait AppContext extends IOApp {

  val arangoConfig = ArangoConfiguration.load()
  val arangoResource = Arango(arangoConfig)

  implicit val cryptoDsl: CryptoAlgebra[IO] = new EthereumCryptoInterpreter[IO]
  implicit val httpFreeDsl: HttpAlgebra[IO] = ioHttpAlgebraInterpreter
  implicit val securityDsl: SecurityAlgebra[IO] = ioJwtSecurityInterpreter
  implicit val storeDsl: ArangoStoreAlgebra[IO] = new ArangoStoreInterpreter(arangoResource)


  val serverConfig = ServerConfig.load()

  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]
}
