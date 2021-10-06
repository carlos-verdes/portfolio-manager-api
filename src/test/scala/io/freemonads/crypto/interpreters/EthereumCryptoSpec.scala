/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package io.freemonads.crypto
package interpreters

import cats.effect.IO
import io.freemonads.error.{NonAuthorizedError, RequestFormatError}
import io.freemonads.specs2.Http4FreeIOMatchers
import io.freemonads.tagless.crypto.CryptoAlgebra
import io.freemonads.tagless.crypto.ethereum.EthereumCryptoInterpreter
import org.specs2.Specification
import org.specs2.matcher.{IOMatchers, MatchResult}
import org.specs2.specification.core.SpecStructure

trait MessagesAndSignatures {

  val msg = "v0G9u7huK4mJb2K1"

  val signature = "0x2c6401216c9031b9a6fb8cbfccab4fcec6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57321b619ed7a" +
      "9cfd38bd973c3e1e0243ac2777fe9d5b01"

  val signature2 = "0x2c6401216c9031b9a6fb8cbfccab4fcec6c951cdf40e2320108d1856eb532250576865fbcd452bcdc4c57321b619ed7" +
      "a9cfd38bd973c3e1e0243ac2777fe9d5b1b"

  val walletAddress = "0x31b26e43651e9371c88af3d36c14cfd938baf4fd"
  val canonicalAddress = "0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd"
  val otherAddress = "0xef678007d18427e6022059dbc264f27507cd1ffc"
  val wrongFormatAddress = "ef678007d18427e6022059dbc264f27507cd1ffc"
}

class EthereumCryptoSpec extends Specification with Http4FreeIOMatchers with IOMatchers with MessagesAndSignatures {
  def is: SpecStructure =
    s2"""
        Ethereum Crypto should:
        Pass valid address and format $passValidAddress
        Reject wrong address          $rejectWrongAddress
        Pass valid signed message     $passValidSignature
        Reject wrong signataure       $rejectWrongSignature
        """

  implicit val dsl: CryptoAlgebra[IO] = new EthereumCryptoInterpreter[IO]

  def passValidAddress: MatchResult[Any] = dsl.validateAddress(walletAddress) must returnValue(canonicalAddress)

  def rejectWrongAddress: MatchResult[Any] =
    dsl.validateAddress(wrongFormatAddress) must returnError[String, RequestFormatError]

  def passValidSignature: MatchResult[Any] =
    dsl.validateMessage(msg, signature, walletAddress) must returnValue(())

  def rejectWrongSignature: MatchResult[Any] =
    dsl.validateMessage(msg, signature2, otherAddress) must returnError[Unit, NonAuthorizedError]
}
