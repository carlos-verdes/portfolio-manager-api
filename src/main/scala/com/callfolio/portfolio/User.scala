/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import java.util.UUID

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.effect.IO
import cats.~>
import io.circe.generic.auto._
import io.freemonads.http.resource.ResourceDsl
import io.freemonads.http.rest._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits.http4sLiteralsSyntax

case class UserRequest(publicAddress: String, userName: Option[String] = None)
case class User(publicAddress: String, userName: Option[String] = None, nounce: String)

object User {

  implicit class UserRequestOps(r: UserRequest) {

    def withNewNounce: User = User(r.publicAddress, r.userName, UUID.randomUUID().toString)
  }

  def userRoutes[Algebra[_], Encoder[_], Decoder[_]](
      implicit http4sFreeDsl: Http4sFreeDsl[Algebra],
      resourceDsl: ResourceDsl[Algebra, Encoder, Decoder],
      encoder: Encoder[User],
      decoder: Decoder[User],
      //cryptoDsl: CryptoDsl[Algebra],
      interpreters: Algebra ~> IO): HttpRoutes[IO] = {

    import http4sFreeDsl._
    import resourceDsl._

    HttpRoutes.of[IO] {
      case r @ GET -> Root / _ =>
        for {
          userWithNonce <- fetch[User](r.uri)
        } yield Ok(userWithNonce.body)
      case r @ POST -> Root =>
        for {
          userRequest <- parseRequest[IO, UserRequest](r)
          // TODO validate user request (valid public address, resource already exist, etc)
          userWithNonce <- store(uri"/users", userRequest.withNewNounce)
        } yield Created(userWithNonce.body, Location(userWithNonce.uri))
    }
  }

  implicit val userSerializer: VPackEncoder[User] = VPackEncoder.gen
  implicit val userDeserializer: VPackDecoder[User] = VPackDecoder.gen
}
