package de.dnpm.dip.authup.client


import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.collection.concurrent.{
  Map,
  TrieMap,
}
import scala.util.Success
import cats.syntax.either._
import play.api.mvc.{
  RequestHeader,
  Result
}
import play.api.mvc.ControllerHelpers.{
  AUTHORIZATION,
  Unauthorized
}
import play.api.libs.ws.WSClient
import play.api.libs.json.JsValue
import de.dnpm.dip.util.Logging
import de.dnpm.dip.util.mapping.syntax._
import de.dnpm.dip.auth.api.{
  UserWithRoles,
  UserAuthenticationService
}



class AuthupClient @Inject()(
  wsclient: WSClient
)
extends UserAuthenticationService
with Logging
{

  private val config =
    Config.instance.get

  /*
   - Configuration of Authup baseURL
   - Create Roles and Permissions
   - Cache, based on expiration time
   - TokenIntrospectionResult => UserWithRoles
  */

  private val executor =
    Executors.newSingleThreadScheduledExecutor


  private implicit val tokenResultToUserWithRoles: TokenIntrospectionResult => UserWithRoles =
    tkn =>
      UserWithRoles(
        tkn.sub,
        tkn.name,
        tkn.given_name,
        tkn.family_name,
        tkn.permissions.map(_.name)
      )
  

  private val cache: Map[String,TokenIntrospectionResult] =
    TrieMap.empty


  private def introspect(
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[TokenIntrospectionResult] =
    wsclient
     .url(s"${config.baseUrl}/token/introspect")
     .withHttpHeaders(AUTHORIZATION -> token)
     .get()
     .map(_.json.as[TokenIntrospectionResult])
     
     
  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserWithRoles]] = {

     req.headers.get(AUTHORIZATION) match {
       case Some(token) =>
         (
           cache.get(token) match {
             case Some(tknInfo) =>
               Future.successful(tknInfo)
           
             case None =>
               introspect(token)
                 .andThen { 
                   case Success(tknInfo) =>
                     cache += token -> tknInfo
                     executor.schedule(
                       () => cache -= token,
                       ChronoUnit.SECONDS.between(
                         Instant.now,
                         Instant.ofEpochSecond(tknInfo.exp).minusSeconds(5)  // remove 5 seconds of the actual expiration time as security buffer
                       ),
                       SECONDS
                     )
                 }
           }
         )
         .map(
           _.mapTo[UserWithRoles]
            .asRight[Result]
          )

       case None =>
         Future.successful(
           Unauthorized("Unauthorized").asLeft[UserWithRoles]
         )

     }

  }

}
