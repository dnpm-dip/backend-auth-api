package de.dnpm.dip.authup.client



import java.time.Instant
import java.time.temporal.ChronoUnit.{SECONDS => SECS}
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.concurrent.duration._
import scala.collection.concurrent.{
  Map,
  TrieMap,
}
import scala.util.{
  Left,
  Right,
  Success,
  Failure
}
import scala.util.chaining._
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
  WSRequest,
  WSResponse,
  WSClient
}
import play.api.libs.json.JsString
import play.api.libs.json.Json.{
  toJson,
  toJsObject
}
import de.dnpm.dip.util.Logging
import de.dnpm.dip.util.mapping.syntax._
import de.dnpm.dip.auth.api.{
  UserPermissions,
  UserAuthenticationService,
  UserAuthenticationSPI
}
import de.dnpm.dip.service.auth.{ 
  Permissions,
  Roles
}
import com.google.inject.AbstractModule


/*
class AuthupClientSPI extends UserAuthenticationSPI
{
  override def getInstance: UserAuthenticationService =
    new AuthupClient()
}
*/

class GuiceModule extends AbstractModule
{
  override def configure = 
    bind(classOf[UserAuthenticationService])
      .to(classOf[AuthupClient])
}


class AuthupClient @Inject()(
  wsclient: WSClient
)
extends UserAuthenticationService
with Logging
{

  private val config =
    Config.instance.get

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
 

  private def request(
    uri: String,
    token: Option[String]
  ): WSRequest =
    wsclient
     .url(s"${config.baseUrl}$uri")
     .withRequestTimeout(10 seconds)
     .pipe { 
       req =>
         token match {
           case Some(tkn) => req.withHttpHeaders(AUTHORIZATION -> tkn)
           case _ => req
         }
     }


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
    request("/token/introspect",Some(token))
      .get()
      .map(
        resp =>
          resp.status match {
            case 200 => resp.json.as[TokenIntrospection].asRight[Result]
            case _   => result(resp).asLeft[TokenIntrospection]
          }
      )
      .andThen {
        case Success(Left(result)) =>
          log.error(s"Token introspection failed with SC ${result.header.status}")

        case Failure(t) =>  
          log.error(s"Token introspection failed",t)
      }
 
     
  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserPermissions]] = {

     req.headers.get(AUTHORIZATION) match {

       case Some(token) =>
         for {
           result <-
             cache.get(token) match {
               case Some(tknInfo) =>
                 Future.successful(tknInfo.asRight[Result])
             
               case None =>
                 introspect(token)
                   .andThen { 
                     case Success(Right(tknInfo)) =>
                       cache += token -> tknInfo

                       // schedule auto-removal of token from cache at expiration time
                       executor.schedule(
                         () => cache -= token,
                         SECS.between(
                           Instant.now,
                           Instant.ofEpochSecond(tknInfo.exp)
                         ),
                         SECONDS
                       )
                   }
             }

         } yield result.map(_.mapTo[UserPermissions])
         
       case None =>
         Future.successful(
           Unauthorized("Unauthorized").asLeft[UserPermissions]
         )

     }

  }


  // --------------------------------------------------------------------------
  // Operations for auto-setup of permission/role model
  // --------------------------------------------------------------------------

  private def loginAuthup(
    credentials: Credentials
  )(
    implicit ec: ExecutionContext
  ): Future[String] =
    request("/token",None)
      .post(
        toJsObject(credentials) + ("grant_type" -> JsString("password"))
      )
      .collect {
        case resp if resp.header.status == 200 =>
          s"Bearer ${(resp.json \ "access_token").as[String]}"
      }
      .andThen {
        case Success(_) => log.debug("Logged into Authup")
      }


  private def createOrGet(
    permission: Permission,
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[CreatedPermission] =
    request("/permissions",Some(token))
     .post(toJson(permission))
     .flatMap {
       resp =>
         resp.status match {
           case 201 =>
             Future.successful(resp.json.as[CreatedPermission])
           case 409 =>
             request("/permissions",Some(token))
               .withQueryStringParameters("filter[name]" -> permission.name)
               .get()
               .map(r => (r.json \ "data" \ 0).as[CreatedPermission])
         }
     }  


  private def createOrGet(
    role: Role,
    permissionIds: Set[String],
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[CreatedRole] =
    for {
      createdRole <-
        request("/roles",Some(token))
         .post(toJson(role))
         .flatMap {
           resp =>
             resp.status match {
               case 201 =>
                 Future.successful(resp.json.as[CreatedRole])
               case 409 =>
                 request("/roles",Some(token))
                   .withQueryStringParameters("filter[name]" -> role.name)
                   .get()
                   .map(r => (r.json \ "data" \ 0).as[CreatedRole])
             }
         }

       rolePermissions <-
         Future.traverse(
             permissionIds.map(
               id =>
                 RolePermission(
                   role_id       = createdRole.id,
                   permission_id = id
                 )
             )
           )(
             rolePermission =>
               request("/role-permissions",Some(token))
                 .post(toJson(rolePermission))
                 .map(
                   resp =>
                     resp.status match {
                       case 201 | 409 => true
                     }
                 )
           )

    } yield createdRole



  override def setupPermissionModel(
    implicit ec: ExecutionContext
  ): Future[Boolean] = 
    config.adminCredentials match {

      case Some(credentials) =>
        {
          for {
            token <-
              loginAuthup(credentials)
        
            permissions <-
              Future.traverse(
                Permissions
                  .getAll
                  .map(_.name)
                  .map(Permission(_))
                )(
                  createOrGet(_,token)
                )
        
            permissionIdsByName =
              permissions
                .map(p => p.name -> p.id)
                .toMap
        
            roles <-
              Future.traverse(
                Roles.getAll
              )(
                role =>
                  createOrGet(
                    Role(role.name),
                    role.permissions
                      .map(_.name)
                      .flatMap(permissionIdsByName.get),
                    token
                  )
             )
              
          } yield true
        }
        .andThen { 
          case Success(_) =>
            log.info("Successfully set up permission/role model")
        
          case Failure(t) =>
            log.error("Error(s) occurred setting up permission/role model", t)
        }

      case None =>
        log.warn("Undefined admin credentials, can't create permission/role model")
        Future.successful(false)  

    }


  private[client] val setupResult =  
    setupPermissionModel(scala.concurrent.ExecutionContext.global)

}
