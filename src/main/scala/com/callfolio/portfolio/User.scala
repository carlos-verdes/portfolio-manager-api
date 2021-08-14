/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.~>
import cats.syntax.option._
import io.circe.generic.auto._
import io.freemonads.api._
import io.freemonads.arango.ArangoDsl
import io.freemonads.crypto.CryptoDsl
import io.freemonads.http.resource._
import io.freemonads.http.rest._
import io.freemonads.security._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthScheme, AuthedRoutes, Credentials, HttpRoutes, Request, Uri}

case class CreateUser(publicAddress: String)
case class UpdateUser(username: Option[String])
case class User(publicAddress: String, username: Option[String] = None, nonce: String)
case class AuthRequest(address: String, signature: String)

object User {

  val USERS_COLLECTION = "users"
  val AUTH_COLLECTION = "auth"

  def userUri(publicAddress: String): Uri = uri"/" / USERS_COLLECTION / publicAddress
  def userUri(user: User): Uri = userUri(user.publicAddress)

  implicit class UserOps(user: User) {
    def withNonce(newNonce: String): User = user.copy(nonce = newNonce)
  }

  def publicUserRoutes[Algebra[_], Encoder[_], Decoder[_]](
      implicit cryptoDsl: CryptoDsl[Algebra],
      http4sFreeDsl: Http4sFreeDsl[Algebra],
      resourceDsl: ResourceDsl[Algebra, Encoder, Decoder],
      securityDsl: SecurityDsl[Algebra],
      encoder: Encoder[User],
      decoder: Decoder[User],
      interpreters: Algebra ~> IO): HttpRoutes[IO] = {

    import cryptoDsl._
    import http4sFreeDsl._
    import resourceDsl._
    import securityDsl._

    HttpRoutes.of[IO] {

      case r @ POST -> Root / USERS_COLLECTION =>
        for {
          userRequest <- parseRequest[IO, CreateUser](r)
          canonicalAddress <- validateAddress(userRequest.publicAddress)
          canonicalUserUri = userUri(canonicalAddress)
          userResource <- fetch[User](canonicalUserUri).recoverWith {
            case _: ResourceNotFoundError =>
              for {
                nonce <- createNonce(canonicalAddress)
                user <- store(canonicalUserUri, User(canonicalAddress, None, nonce))
              } yield user
          }
        } yield userResource.created[IO]

      case r @ POST -> Root / AUTH_COLLECTION =>
        for {
          AuthRequest(address, signature) <- parseRequest[IO, AuthRequest](r)
          canonicalAddress <- validateAddress(address)
          userUri = User.userUri(canonicalAddress)
          RestResource(_, user) <- fetch[User](userUri)
          newNonce <- createNonce(canonicalAddress)
          _ <- store[User](userUri, user.withNonce(newNonce))
          _ <- validateMessage(user.nonce, signature, canonicalAddress)
          token <- createToken(Subject(canonicalAddress))
        } yield Ok((), Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
    }
  }

  def privateUserRoutes[Algebra[_]](
      implicit http4sFreeDsl: Http4sFreeDsl[Algebra],
      resourceDsl: ResourceDsl[Algebra, VPackEncoder, VPackDecoder],
      interpreters: Algebra ~> IO): AuthedRoutes[User, IO] = {

    val dsl = new Http4sDsl[IO]{}
    import dsl._
    import http4sFreeDsl._
    import resourceDsl._

    val wrongAddressError = NonAuthorizedError("wrong address".some).resultError

    AuthedRoutes.of[User, IO] {
      case GET -> Root / "profile" as user =>
        for {
          userFree <- user.resultOk.liftFree[Algebra]
        } yield Ok(userFree)

      case r @ PUT -> Root / "users" / address as user =>
        for {
          UpdateUser(newUsername) <- parseRequest[IO, UpdateUser](r.req)
          _ <- (if (address == user.publicAddress) ().resultOk else wrongAddressError).liftFree[Algebra]
          updatedUser <- store[User](userUri(user), user.copy(username = newUsername))
        } yield updatedUser.ok[IO]
    }
  }

  def jwtTokenFromRequest(request: Request[IO]): ApiResult[String] =
    request.headers.get[Authorization] match {
      case Some(credentials) => credentials match {
        case Authorization(Credentials.Token(_, jwtToken)) => jwtToken.resultOk
        case _ => NonAuthorizedError("Invalid Authorization header".some).resultError[String]
      }
      case None => NonAuthorizedError("Couldn't find an Authorization header".some).resultError[String]
    }

  def authUser[Algebra[_]](
      implicit resourceDsl: ArangoDsl[Algebra],
      securityDsl: SecurityDsl[Algebra],
      interpreters: Algebra ~> IO): Kleisli[IO, Request[IO], ApiResult[User]] =

    Kleisli({ request =>
      val message = for {
        jwtToken <- jwtTokenFromRequest(request).liftFree[Algebra]
        claim <- securityDsl.validateToken(Token(jwtToken))
        user <- resourceDsl.fetch[User](userUri(claim.subject.value))
      } yield user.body

      message.value.foldMap(interpreters)
    })

  val onFailure: AuthedRoutes[ApiError, IO] = Kleisli(req => OptionT.liftF(req.context))

  def authMiddleware[Algebra[_]](
      implicit resourceDsl: ResourceDsl[Algebra, VPackEncoder, VPackDecoder],
      securityDsl: SecurityDsl[Algebra],
      interpreters: Algebra ~> IO): AuthMiddleware[IO, User] = AuthMiddleware(authUser, onFailure)


  implicit val userSerializer: VPackEncoder[User] = VPackEncoder.gen
  implicit val userDeserializer: VPackDecoder[User] = VPackDecoder.gen
}
