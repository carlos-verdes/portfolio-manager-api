/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio


import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.option._
import io.circe.generic.auto._
import cats.implicits.catsSyntaxApplicativeError
import io.freemonads.error.{NonAuthorizedError, ResourceNotFoundError}
import io.freemonads.tagless.crypto.CryptoAlgebra
import io.freemonads.tagless.http.{HttpAlgebra, HttpResource}
import io.freemonads.tagless.interpreters.arangoStore.ArangoStoreAlgebra
import io.freemonads.tagless.security.{SecurityAlgebra, Subject, Token}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.AuthMiddleware
import org.http4s._

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

  def publicUserRoutes(
      implicit cryptoDsl: CryptoAlgebra[IO],
      http4sFreeDsl: HttpAlgebra[IO],
      resourceDsl: ArangoStoreAlgebra[IO],
      securityDsl: SecurityAlgebra[IO]): HttpRoutes[IO] = {

    import cryptoDsl._
    import http4sFreeDsl._
    import resourceDsl._
    import securityDsl._

    HttpRoutes.of[IO] {

      case r @ POST -> Root / USERS_COLLECTION =>
        for {
          userRequest <- parseRequest[CreateUser](r)
          canonicalAddress <- validateAddress(userRequest.publicAddress)
          canonicalUserUri = userUri(canonicalAddress)
          userResource <- fetch[User](canonicalUserUri).recoverWith {
            case _: ResourceNotFoundError =>
              for {
                nonce <- createNonce(canonicalAddress)
                user <- store(canonicalUserUri, User(canonicalAddress, None, nonce))
              } yield user
          }
        } yield  userResource.created[IO]

      case r @ POST -> Root / AUTH_COLLECTION =>
        for {
          AuthRequest(address, signature) <- parseRequest[AuthRequest](r)
          canonicalAddress <- validateAddress(address)
          userUri = User.userUri(canonicalAddress)
          HttpResource(_, user) <- fetch[User](userUri)
          newNonce <- createNonce(canonicalAddress)
          _ <- store[User](userUri, user.withNonce(newNonce))
          _ <- validateMessage(user.nonce, signature, canonicalAddress)
          token <- createToken(Subject(canonicalAddress))
          response <-  Ok((), Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        } yield response
    }
  }

  def privateUserRoutes(
      implicit http4sFreeDsl: HttpAlgebra[IO],
      storeDsl: ArangoStoreAlgebra[IO]): AuthedRoutes[User, IO] = {

    import http4sFreeDsl._
    import storeDsl._

    val wrongAddressError = NonAuthorizedError("wrong address".some)

    AuthedRoutes.of[User, IO] {
      case GET -> Root / "profile" as user => Ok(user)

      case r @ PUT -> Root / "users" / address as user =>
        for {
          UpdateUser(newUsername) <- parseRequest[UpdateUser](r.req)
          _ <- if (address == user.publicAddress) IO.pure(()) else IO.raiseError(wrongAddressError)
          updatedUser <- store[User](userUri(user), user.copy(username = newUsername))
        } yield updatedUser.ok[IO]
    }
  }

  def authUser(
      implicit httpFreeDsl: HttpAlgebra[IO],
      storeDsl: ArangoStoreAlgebra[IO],
      securityDsl: SecurityAlgebra[IO]): Kleisli[IO, Request[IO], Either[Throwable, User]] =

    Kleisli({ request =>
      val message = for {

        jwtToken <- httpFreeDsl.getJwtTokenFromHeader(request)
        claim <- securityDsl.validateToken(Token(jwtToken))
        user <- storeDsl.fetch[User](userUri(claim.subject.value))
      } yield user.body

      message.attempt
    })

  val onFailure: AuthedRoutes[Throwable, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.context.toString)))

  def authMiddleware(
      implicit httpFreeDsl: HttpAlgebra[IO],
      storeDsl: ArangoStoreAlgebra[IO],
      securityDsl: SecurityAlgebra[IO]): AuthMiddleware[IO, User] = AuthMiddleware[IO, Throwable, User](authUser, onFailure)


  implicit val userSerializer: VPackEncoder[User] = VPackEncoder.gen
  implicit val userDeserializer: VPackDecoder[User] = VPackDecoder.gen
}
