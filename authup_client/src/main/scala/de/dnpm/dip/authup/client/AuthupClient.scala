package de.dnpm.dip.authup.client


import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.collection.concurrent.{
  Map,
  TrieMap,
}
import scala.util.matching.Regex
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
//import play.api.libs.json.Json.fromJson
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
  
//  private val bearer =
//    raw"Bearer\s(\.+)".r


  private val cache: Map[String,Future[TokenIntrospectionResult]] =
    TrieMap.empty


  private implicit val tokenResultToUSerWithRoles: TokenIntrospectionResult => UserWithRoles =
    tkn =>
      UserWithRoles(
        tkn.sub,
        tkn.name,
        tkn.given_name,
        tkn.family_name,
        tkn.permissions.map(_.name)
      )

  
  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserWithRoles]] = {

     req.headers.get(AUTHORIZATION) match {
       case Some(token) =>
         wsclient
          .url(s"${config.baseUrl}/token/introspect")
          .withHttpHeaders(AUTHORIZATION -> token)
          .get()
          .map(
//            _.body[JsValue]
            _.json
             .as[TokenIntrospectionResult]
             .mapTo[UserWithRoles]
             .asRight[Result]
           )

       case None =>
         Future.successful(
           Unauthorized("Unauthorized").asLeft[UserWithRoles]
         )

     }

  }

}
