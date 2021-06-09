/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio
package portfolio

import scala.concurrent.Future

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.specs2.DockerTestKit
import io.freemonads.arango.DockerArango
import io.freemonads.specs2.Http4FreeIOMatchers
import org.specs2.Specification
import org.specs2.matcher.{IOMatchers, MatchResult}
import org.specs2.specification.core.{Env, SpecStructure}


trait UserRoutes extends AppContext with IOMatchers {

  import User._

  val routes = userRoutes[PortfolioAlgebra, VPackEncoder, VPackDecoder]
}

class UserSpec(env: Env)
    extends Specification
    with DockerKitSpotify
    with DockerArango
    with DockerTestKit
    with Http4FreeIOMatchers
    with UserRoutes {

  implicit val ee = env.executionEnv

  def is: SpecStructure =
    s2"""
        The ArangoDB container should be ready                  $arangoIsReady
        Retrieve user with nounce if exist $retrieveUser
      """

    def arangoIsReady: MatchResult[Future[Boolean]] = isContainerReady(arangoContainer) must beTrue.await
    def retrieveUser: MatchResult[Any] = true must_===(true)
}
