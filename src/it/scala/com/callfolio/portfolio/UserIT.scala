/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio

import java.security.{NoSuchAlgorithmException, SecureRandom, Security}
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
import org.specs2.matcher.{IOMatchers, MatchResult}
import org.specs2.specification.core.{Env, SpecStructure}


trait UserRoutes extends AppContext with IOMatchers {

  import User._

  val routes = publicUserRoutes[PortfolioAlgebra, VPackEncoder, VPackDecoder]

  val userAddress1 = "0xef678007d18427e6022059dbc264f27507cd1ffc"
  val canonicalAddress1 = "0xef678007D18427E6022059Dbc264f27507CD1ffC"
  val userAddress2 = "0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"
  val userAddress3 = "0x44A84615dD457f729bbbf85f009F3d2e8d484D91"

  val testNonce = "v0G9u7huK4mJb2K1"
  val signature = "0x2c6401216c9031b9a6fb8cbfccab4fcec6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57321b619ed7a" +
      "9cfd38bd973c3e1e0243ac2777fe9d5b01"

  val userRequest = routes.orNotFound

  val authRequestUser2 = AuthRequest(userAddress2, signature)
  val badAuthRequestUser3 = AuthRequest(userAddress3, signature)

  val userWrongAddress = CreateUser("0xef678007d18427e6022059dbc264f27507cd1ffc")
  val wrongPathRequest = routes.orNotFound(Request[IO](Method.POST, uri"/aaa"))

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
}

class UserIT(env: Env)
    extends Specification
    with DockerKitSpotify
    with DockerArango
    with DockerTestKit
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
        Validate and change a signed nonce (authentication)    $authentication
        Change nonce after wrong authentication                $resetNonce
      """

  val resourceDsl = ResourceDsl.instance[PortfolioAlgebra, VPackEncoder, VPackDecoder]

  def arangoIsReady: MatchResult[Future[Boolean]] = isContainerReady(arangoContainer) must beTrue.await

  def createUser: MatchResult[Any] = {

    val newUserReq = userRequest(Request[IO](POST, uri"/users").withEntity[CreateUser](CreateUser(userAddress1)))

    newUserReq must returnValue { (response: Response[IO]) =>
      (response must haveStatus(Created)) and (response must containHeader(Location(uri"/users" / canonicalAddress1)))
    }
  }

  def authentication: MatchResult[Any] = {

    val userWithNonce: User = User(userAddress2, None, testNonce)
    val userResource = RestResource(User.userUri(userAddress2), userWithNonce)

    val createResourceRequest = resourceDsl.store[User](User.userUri(userAddress2), userWithNonce)
    val authReq = routes.orNotFound(Request[IO](Method.POST, uri"/auth").withEntity[AuthRequest](authRequestUser2))

    val getUserRequest = resourceDsl.fetch[User](User.userUri(userAddress2))

    (createResourceRequest must resultOk(userResource)) and
        (authReq must returnStatus(Ok)) and
        (getUserRequest.map(_.body.nonce) must not (resultOk(testNonce)))
  }

  def resetNonce: MatchResult[Any] = {

    val userWithNonce: User = User(userAddress3, None, testNonce)
    val userResource = RestResource(User.userUri(userAddress3), userWithNonce)

    val createResourceRequest = resourceDsl.store[User](User.userUri(userAddress3), userWithNonce)
    val authReq = routes.orNotFound(Request[IO](Method.POST, uri"/auth").withEntity[AuthRequest](badAuthRequestUser3))

    val getUserRequest = resourceDsl.fetch[User](User.userUri(userAddress3))

    (createResourceRequest must resultOk(userResource)) and
        (authReq must returnStatus(Forbidden)) and
        (getUserRequest.map(_.body.nonce) must not (resultOk(testNonce)))

  }

  def wrongPathNotFound: MatchResult[Any] = wrongPathRequest must returnStatus(NotFound)
}
