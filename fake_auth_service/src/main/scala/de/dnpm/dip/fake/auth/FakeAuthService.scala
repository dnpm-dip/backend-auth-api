package de.dnpm.dip.fake.auth


import scala.util.Failure
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  RequestHeader,
  Result
}
import cats.syntax.either._
import de.dnpm.dip.util.Logging
import de.dnpm.dip.auth.api.{
  UserPermissions,
  UserAuthenticationService,
  UserAuthenticationSPI
}
import de.dnpm.dip.service.auth.{
  Permissions
}


class FakeAuthSPI extends UserAuthenticationSPI
{
  override def getInstance: UserAuthenticationService =
    FakeAuthService.instance
}


object FakeAuthService
{
  lazy val instance =
    new FakeAuthService
}

class FakeAuthService
extends UserAuthenticationService
with Logging
{

  import scala.concurrent.ExecutionContext.Implicits._

  private val userPermissions: Future[Either[Result,UserPermissions]] =
    Future {
      UserPermissions(
        "Dummy-User-ID",
        "dummy_user",
        "Dummy",
        "User",
        Permissions
          .getAll
          .map(_.name)
          .toSet
      )
    }
    .map(_.asRight)
    .andThen {
      case Failure(t) =>
        log.error("Fake Authentication Service: Failed to initialize dummy user with full permissions",t)
    }


  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserPermissions]] = {

    log.warn(
      "Fake Authentication Service in operation! This always returns a DUMMY USER with FULL PERMISSIONS, and is meant for TESTING purposes only!"
    )

    userPermissions
  }

  override  def setupPermissionModel(
    implicit ec: ExecutionContext
  ): Future[Boolean] =
    Future.successful(true)

}
