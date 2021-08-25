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
case class CreateNonce(address: String) extends CryptoAlgebra[ApiResult[String]]

class CryptoDsl[Algebra[_]](implicit I: InjectK[CryptoAlgebra, Algebra]) {

  def validateAddress[R](address: String): ApiFree[Algebra, String] = EitherT(inject(ValidateAddress(address)))

  def validateMessage[R](msg: String, signature: String, address: String): ApiFree[Algebra, Unit] =
    EitherT(inject(ValidateMessage(msg, signature, address)))

  def createNonce(address: String): ApiFree[Algebra, String] = EitherT(inject(CreateNonce(address)))

  private def inject = Free.liftInject[Algebra]
}

object CryptoDsl {

  implicit def instance[F[_]](implicit I: InjectK[CryptoAlgebra, F]): CryptoDsl[F] = new CryptoDsl[F]
}
