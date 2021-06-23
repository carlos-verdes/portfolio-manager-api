/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package io.freemonads.crypto
package interpreters

import cats.{Id, ~>}
import io.freemonads.api._
import io.freemonads.specs2.Http4FreeMatchers
import org.specs2.Specification
import org.specs2.matcher.MatchResult
import org.specs2.specification.core.SpecStructure

import scala.concurrent.duration.FiniteDuration

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

  implicit val basicInterpreter: CryptoAlgebra ~> Id = ethereumCryptoInterpreter[Id]
}

class EthereumCryptoSpec extends Specification with Http4FreeMatchers[Id] with MessagesAndSignatures {
  def is: SpecStructure =
    s2"""
        Ethereum Crypto should:
        Pass valid address and format $passValidAddress
        Reject wrong address          $rejectWrongAddress
        Pass valid signed message     $passValidSignature
        Reject wrong signataure       $rejectWrongSignature
        """

  implicit val dsl = CryptoDsl.instance[CryptoAlgebra]

  def passValidAddress: MatchResult[Any] = dsl.validateAddress(walletAddress) must resultOk(canonicalAddress)

  def rejectWrongAddress: MatchResult[Any] =
    dsl.validateAddress(wrongFormatAddress) must resultError[CryptoAlgebra, String, RequestFormatError]

  def passValidSignature: MatchResult[Any] =
    dsl.validateMessage(msg, signature, walletAddress) must resultOk(())

  def rejectWrongSignature: MatchResult[Any] =
    dsl.validateMessage(msg, signature2, otherAddress) must resultError[CryptoAlgebra, Unit, RequestFormatError]

  override protected def runWithTimeout[A](fa: Id[A], timeout: FiniteDuration): A = fa
  override protected def runAwait[A](fa: Id[A]): A = fa
}
