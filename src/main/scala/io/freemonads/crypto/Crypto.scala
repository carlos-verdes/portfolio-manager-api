/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package io.freemonads
package crypto

import cats.InjectK
import cats.data.EitherT
import cats.free.Free
import io.freemonads.api._


sealed trait CryptoAlgebra[Result]
case class ValidateAddress(address: String) extends CryptoAlgebra[ApiResult[String]]
case class ValidateMessage(msg: String, signature: String, publicKey: String) extends CryptoAlgebra[ApiResult[Unit]]

class CryptoDsl[F[_]](implicit I: InjectK[CryptoAlgebra, F]) {

  def validateAddress[R](address: String): ApiFree[F, String] = EitherT(inject(ValidateAddress(address)))

  def validateMessage[R](msg: String, signature: String, publicKey: String): ApiFree[F, Unit] =
    EitherT(inject(ValidateMessage(msg, signature, publicKey)))

  private def inject = Free.inject[CryptoAlgebra, F]
}

object CryptoDsl {

  implicit def instance[F[_]](implicit I: InjectK[CryptoAlgebra, F]): CryptoDsl[F] = new CryptoDsl[F]
}
