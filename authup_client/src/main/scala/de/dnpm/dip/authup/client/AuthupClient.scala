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
import scala.util.{
  Right,
  Success
}
import cats.syntax.either._
import play.api.http.HttpEntity
import play.api.mvc.{
  RequestHeader,
  ResponseHeader,
  Result
}
import play.api.mvc.ControllerHelpers.{
  AUTHORIZATION,
  Unauthorized
}
import play.api.libs.ws.{
  WSResponse,
  WSClient
}
import play.api.libs.json.JsValue
import de.dnpm.dip.util.Logging
import de.dnpm.dip.util.mapping.syntax._
import de.dnpm.dip.auth.api.{
  UserPermissions,
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
   TODO:
   - Create Roles and Permissions
   - Configuration of Authup admin credentials for role/permission creation
  */

  private val cache: Map[String,TokenIntrospection] =
    TrieMap.empty

  private val executor =
    Executors.newSingleThreadScheduledExecutor


  private implicit val tokenResultToUserPermissions: TokenIntrospection => UserPermissions =
    tkn =>
      UserPermissions(
        tkn.sub,
        tkn.name,
        tkn.given_name,
        tkn.family_name,
        tkn.permissions.map(_.name)
      )
  

  //Inspired from: https://stackoverflow.com/questions/31135734/how-to-forward-a-wsresponse-without-explicitly-mapping-the-result
  private def result(
    response: WSResponse
  ): Result =
    Result(
      ResponseHeader(
        response.status,
        response.headers
          .map { case (name,values) => name -> values.head }
      ),
      // No large payload to be expected here, so no need for streaming of the body
      HttpEntity.Strict( 
        response.bodyAsBytes,
        Some(response.contentType)
      )
    )


  private def introspect(
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,TokenIntrospection]] =
    wsclient
     .url(s"${config.baseUrl}/token/introspect")
     .withHttpHeaders(AUTHORIZATION -> token)
     .get()
     .map(
       resp =>
         resp.status match {
           case 200 =>
             resp.json.as[TokenIntrospection].asRight[Result]

           case _ =>
             result(resp).asLeft[TokenIntrospection]
         }
     )
     
     
  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserPermissions]] = {

     req.headers.get(AUTHORIZATION) match {
       case Some(token) =>
         (
           cache.get(token) match {
             case Some(tknInfo) =>
               Future.successful(tknInfo.asRight[Result])
           
             case None =>
               introspect(token)
                 .andThen { 
                   case Success(Right(tknInfo)) =>
                     cache += token -> tknInfo
                     executor.schedule(
                       () => cache -= token,
                       ChronoUnit.SECONDS.between(
                         Instant.now,
                         Instant.ofEpochSecond(tknInfo.exp).minusSeconds(2)  // remove 2 seconds of the actual expiration time as security buffer
                       ),
                       SECONDS
                     )
                 }
           }
         )
         .map(
           _.map(_.mapTo[UserPermissions])
          )

       case None =>
         Future.successful(
           Unauthorized("Unauthorized").asLeft[UserPermissions]
         )

     }

  }

}
