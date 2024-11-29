package de.dnpm.dip.authup.client


import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
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
  OK,
  Unauthorized,
}
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.ws.ahc.{
  StandaloneAhcWSClient => WSClient
}
import play.api.libs.ws.{
  StandaloneWSRequest => WSRequest,
  StandaloneWSResponse => WSResponse
}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.json.{
  JsString,
  JsValue
}
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



class StandaloneAuthupClientSPI extends UserAuthenticationSPI
{
  override def getInstance: UserAuthenticationService =
    StandaloneAuthupClient.instance
}


object StandaloneAuthupClient
{

  private lazy implicit val system: ActorSystem =
    ActorSystem()

  private lazy implicit val materializer: Materializer =
    Materializer.matFromSystem

  private lazy val client =
    WSClient()

  lazy val instance =
    new StandaloneAuthupClient(client)
}



class StandaloneAuthupClient
(
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
            case 200 => resp.body[JsValue].as[TokenIntrospection].asRight // if active
            case _   => result(resp).asLeft
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
               case Some(tknIntro) =>
                 Future.successful(
                   if (tknIntro.active) tknIntro.asRight
                   else Unauthorized.asLeft
                 )
               case None =>
                 introspect(token)
                   .andThen { 
                     case Success(Right(tknIntro)) =>
                       cache += token -> tknIntro

                       // schedule auto-removal of token from cache at expiration time
                       executor.schedule(
                         () => cache -= token,
                         60,
                         SECONDS
                       )
                   }
                   .map(
                     _.filterOrElse(_.active,Unauthorized)
                   )
             }

         } yield result.map(_.mapTo[UserPermissions])
         
       case None =>
         Future.successful(Unauthorized.asLeft)

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
        credentials match {
          case _: RobotCredentials =>
            toJsObject(credentials) + ("grant_type" -> JsString("robot_credentials"))

          case _: UserCredentials  =>
            toJsObject(credentials) + ("grant_type" -> JsString("password"))
        }
      )
      .collect {
        case resp if resp.status == OK =>
          s"Bearer ${(resp.body[JsValue] \ "access_token").as[String]}"
      }
      .andThen { 
        case Success(_) => log.debug("Logged into Authup")
      }


  private def createOrGet(
    permission: Permission,
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[CreatedPermission] = {

    log.debug(s"Setting up permission '${permission.name}'")

    request(s"/permissions/${permission.name}",Some(token))
     .put(toJson(permission))
     .collect {
       case resp if resp.status == 201 || resp.status == 202 =>
         resp.body[JsValue].as[CreatedPermission]
     }  
  }


  private def createOrGet(
    role: Role,
    permissionIds: Set[String],
    token: String
  )(
    implicit ec: ExecutionContext
  ): Future[CreatedRole] = {

    log.debug(s"Setting up role '${role.name}'")

    for {
      createdRole <-
        request(s"/roles/${role.name}",Some(token))
         .put(toJson(role))
         .collect {
            case resp if resp.status == 201 || resp.status == 202 =>
              resp.body[JsValue].as[CreatedRole]
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
                 .collect {
                   case resp if resp.status == 201 || resp.status == 409 => true
                 }
           )

    } yield createdRole

  }

    
  override def setupPermissionModel(
    implicit ec: ExecutionContext
  ): Future[Boolean] = 
    config.adminCredentials match {

      case Some(credentials) =>
        {
          log.info("Setting up permission/role model")
          for {
            token <-
              loginAuthup(credentials)
        
            permissions <-
              Future.traverse(
                Permissions.getAll
              )(
                perm =>
                  createOrGet(
                    Permission(
                      perm.name,
                      Some(perm.display),
                      perm.description
                    ),
                    token
                  )
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
                    Role(
                      role.name,
                      role.display,
                      role.description
                    ),
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
