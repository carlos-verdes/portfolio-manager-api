/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package io.freemonads
package crypto

import java.math.BigInteger

import cats.{Applicative, ~>}
import cats.syntax.applicative._
import org.web3j.crypto.Keys.toChecksumAddress
import org.web3j.crypto.{ECDSASignature, Hash, Keys}
import org.web3j.crypto.Sign.{SignatureData, recoverFromSignature}
import org.web3j.utils.Numeric.hexStringToByteArray

package object interpreters {

  import api._

  val ETH_ADDRESS_REGEX = "^0x[0-9a-f]{40}$".r
  val PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"
  val SIGNATURE_ERROR = RequestFormatError(Some(s"Signature doesn't match"))
  val WRONG_ETH_ADDRESS_ERROR = RequestFormatError(Some("Invalid address"))

  val INDEX_0 = 0
  val NUMBER_27 = 27
  val INDEX_32 = 32
  val INDEX_64 = 64

  def ethereumCryptoInterpreter[F[_] : Applicative]: CryptoAlgebra ~> F = new (CryptoAlgebra ~> F) {
    override def apply[A](op: CryptoAlgebra[A]): F[A] = (op match {
      case ValidateAddress(address) => validateAddress(address)
      case ValidateMessage(msg, signature, address) => validateSignedMessage(msg, signature, address)
    }).asInstanceOf[A].pure
  }

  private def canonical(address: String): String = toChecksumAddress(address)

  def validateAddress(address: String): ApiResult[String] =
    if (ETH_ADDRESS_REGEX.matches(address.toLowerCase)) canonical(address).resultOk else WRONG_ETH_ADDRESS_ERROR

  def validateSignedMessage(msg: String, signature: String, address: String): ApiResult[Unit] = {

    val prefixedMessage = PERSONAL_MESSAGE_PREFIX + msg.length + msg
    val messageHash = Hash.sha3(prefixedMessage.getBytes)
    val canonicalAddress = toChecksumAddress(address)

    val signatureBytes = hexStringToByteArray(signature)
    val aux = signatureBytes(INDEX_64)

    val v: Byte = if (aux < NUMBER_27.toByte) (aux + NUMBER_27.toByte).toByte else aux
    val r = java.util.Arrays.copyOfRange(signatureBytes, INDEX_0, INDEX_32)
    val s = java.util.Arrays.copyOfRange(signatureBytes, INDEX_32, INDEX_64)

    val sd = new SignatureData(v, r, s)
    val ecdaSignature = new ECDSASignature(new BigInteger(1, sd.getR), new BigInteger(1, sd.getS))

    var found = false
    var i = 0
    while(!found && i < 4) {
      val candidate = recoverFromSignature(i, ecdaSignature, messageHash)

      if (candidate != null && canonical("0x" + Keys.getAddress(candidate)) == canonicalAddress) found = true

      i = i + 1
    }

    if (found) ().resultOk else SIGNATURE_ERROR.resultError[Unit]
  }
}
