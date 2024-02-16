package de.dnpm.dip.auth.api



import scala.concurrent.{
  ExecutionContext,
  Future
}
import play.api.mvc.{
  AnyContent,
  Request,
  RequestHeader,
  Result
}
import de.dnpm.dip.util.{
  SPI,
  SPILoader
}


/*

  Based on examples found in Play documentation on Action Composition:
 
  https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

  combined with inspiration drawn from Silhouette:

  https://github.com/mohiva/play-silhouette

*/

trait AuthenticationService[Agent]
{

  def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,Agent]]

}


trait UserAuthenticationService extends AuthenticationService[UserPermissions]

trait UserAuthenticationSPI extends SPI[UserAuthenticationService]

object UserAuthenticationService extends SPILoader[UserAuthenticationSPI]


