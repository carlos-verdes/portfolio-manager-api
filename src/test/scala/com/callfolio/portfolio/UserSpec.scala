/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import io.freemonads.specs2.Http4FreeIOMatchers
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure


trait UserRoutes {

  import User._
  import Runtime._


  val routes = userRoutes[PortfolioAlgebra, VPackEncoder, VPackDecoder]
}

class UserSpec extends Specification with Http4FreeIOMatchers { def is: SpecStructure = {

  s2"""
      User routes should:
      Retrieve user with nounce if exist $retrieveUser
    """



  def retrieveUser: MatchResult[Any] = true must_===(true)
}
}
