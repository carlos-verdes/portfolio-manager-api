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
import io.freemonads.http.resource.{ResourceDsl, RestResource}
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

  val userAddress1 = "0xef678007d18427e6022059dbc264f27507cd1ffc"
  val canonicalAddress1 = "0xef678007D18427E6022059Dbc264f27507CD1ffC"
  val userAddress2 = "0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"

  val testNonce = "v0G9u7huK4mJb2K1"
  val signature = "0x2c6401216c9031b9a6fb8cbfccab4fcec6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57321b619ed7a" +
      "9cfd38bd973c3e1e0243ac2777fe9d5b01"

  val userRequest = routes.orNotFound

  val authRequest = AuthRequest(userAddress2, signature)

  val userWrongAddress = UserRequest("0xef678007d18427e6022059dbc264f27507cd1ffc")
  val wrongPathRequest = routes.orNotFound(Request[IO](Method.POST, uri"/aaa"))
}

class UserSpec(env: Env)
    extends Specification
    with DockerKitSpotify
    with DockerArango
    with DockerTestKit
    with Http4sMatchers[IO]
    with Http4FreeIOMatchers
    with IOMatchers
    with AppContext
    with UserRoutes {

  implicit val ee = env.executionEnv

  import org.http4s.dsl.io._

  def is: SpecStructure =
    s2"""
        The ArangoDB container should be ready                 $arangoIsReady
        Create user with nonce                                 $createUser
        Should not match wrong path                            $wrongPathNotFound
        Validate a signed nonce (for authentication)           $authentication
      """

  val resourceDsl = ResourceDsl.instance[PortfolioAlgebra, VPackEncoder, VPackDecoder]

  def arangoIsReady: MatchResult[Future[Boolean]] = isContainerReady(arangoContainer) must beTrue.await

  def createUser: MatchResult[Any] = {

    val newUserReq = userRequest(Request[IO](POST, uri"/users").withEntity[UserRequest](UserRequest(userAddress1)))

    newUserReq must returnValue { (response: Response[IO]) =>
      (response must haveStatus(Created)) and (response must containHeader(Location(uri"/users" / canonicalAddress1)))
    }
  }

  def authentication: MatchResult[Any] = {

    val userWithNonce: User = User(userAddress2, None, testNonce)
    val userResource = RestResource(User.userUri(userAddress2), userWithNonce)

    val createResourceRequest = resourceDsl.store[User](User.userUri(userAddress2), userWithNonce)
    val authReq = routes.orNotFound(Request[IO](Method.POST, uri"/auth").withEntity[AuthRequest](authRequest))

    (createResourceRequest must resultOk(userResource)) and (authReq must returnStatus(Ok))
  }

  def wrongPathNotFound: MatchResult[Any] = wrongPathRequest must returnStatus(NotFound)
}
