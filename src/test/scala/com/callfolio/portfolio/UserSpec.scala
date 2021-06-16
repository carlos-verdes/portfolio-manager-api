/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio

import scala.concurrent.Future

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.effect.IO
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.specs2.DockerTestKit
import io.circe.generic.auto._
import io.freemonads.arango.DockerArango
import io.freemonads.specs2.Http4FreeIOMatchers
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.specs2.Specification
import org.specs2.matcher.{Http4sMatchers, IOMatchers, MatchResult}
import org.specs2.specification.core.{Env, SpecStructure}


trait UserRoutes extends AppContext with IOMatchers {

  import User._

  val routes = userRoutes[PortfolioAlgebra, VPackEncoder, VPackDecoder]

  val userAddress = "0x31b26e43651e9371c88af3d36c14cfd938baf4fd"
  val canonicalAddress = "0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"

  val validUser = UserRequest(userAddress)
  val userWrongAddress = UserRequest("0xef678007d18427e6022059dbc264f27507cd1ffc")

  def createUserRequest(user: UserRequest): IO[Response[IO]] =
    routes.orNotFound(Request[IO](Method.POST, uri"/users").withEntity[UserRequest](user))
}

class UserSpec(env: Env)
    extends Specification
    with DockerKitSpotify
    with DockerArango
    with DockerTestKit
    with Http4sMatchers[IO]
    with Http4FreeIOMatchers
    with IOMatchers
    with UserRoutes {

  implicit val ee = env.executionEnv

  import org.http4s.dsl.io._

  def is: SpecStructure =
    s2"""
        The ArangoDB container should be ready                  $arangoIsReady
        Create user with nonce                                 $createUser
      """

    def arangoIsReady: MatchResult[Future[Boolean]] = isContainerReady(arangoContainer) must beTrue.await

  def createUser: MatchResult[Any] =
    createUserRequest(validUser) must returnValue { (response: Response[IO]) =>
      (response must haveStatus(Created)) and (response must containHeader(Location(uri"/users" / canonicalAddress)))
    }
}
