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
import io.freemonads.api._
import io.freemonads.crypto.CryptoDsl
import io.freemonads.http.resource.{ResourceDsl, RestResource}
import io.freemonads.http.rest._
import org.http4s.{HttpRoutes, Uri}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits.http4sLiteralsSyntax

case class UserRequest(publicAddress: String)
case class User(publicAddress: String, userName: Option[String] = None, nonce: String)
case class AuthRequest(address: String, signature: String)

object User {

  val USERS_COLLECTION = "users"
  val AUTH_COLLECTION = "auth"

  def userUri(publicAddress: String): Uri = uri"/" / USERS_COLLECTION / publicAddress

  def apply(publicAddress: String): User = User(publicAddress, None, UUID.randomUUID().toString)

  def userRoutes[Algebra[_], Encoder[_], Decoder[_]](
      implicit cryptoDsl: CryptoDsl[Algebra],
      http4sFreeDsl: Http4sFreeDsl[Algebra],
      resourceDsl: ResourceDsl[Algebra, Encoder, Decoder],
      encoder: Encoder[User],
      decoder: Decoder[User],
      interpreters: Algebra ~> IO): HttpRoutes[IO] = {

    import cryptoDsl._
    import http4sFreeDsl._
    import resourceDsl._

    HttpRoutes.of[IO] {

      case r @ POST -> Root / USERS_COLLECTION =>
        for {
          userRequest <- parseRequest[IO, UserRequest](r)
          canonicalAddress <- validateAddress(userRequest.publicAddress)
          canonicalUserUri = userUri(canonicalAddress)
          RestResource(userUri, user) <- fetch[User](canonicalUserUri).recoverWith {
            case _: ResourceNotFoundError => store(canonicalUserUri, User(canonicalAddress))
          }
        } yield Created(user, Location(userUri))

      case r @ POST -> Root / AUTH_COLLECTION =>
        for {
          AuthRequest(address, signature) <- parseRequest[IO, AuthRequest](r)
          canonicalAddress <- validateAddress(address)
          userResource <- fetch[User](User.userUri(canonicalAddress))
          _ <- validateMessage(userResource.body.nonce, signature, canonicalAddress)
        } yield Ok()

    }
  }

  implicit val userSerializer: VPackEncoder[User] = VPackEncoder.gen
  implicit val userDeserializer: VPackDecoder[User] = VPackDecoder.gen
}
