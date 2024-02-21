package de.dnpm.dip.fake.auth


import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  RequestHeader,
  Result
}
import com.google.inject.AbstractModule
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
    new FakeAuthService
}


class GuiceModule extends AbstractModule
{
  override def configure =
    bind(classOf[UserAuthenticationService])
      .to(classOf[FakeAuthService])
}


class FakeAuthService
extends UserAuthenticationService
with Logging
{

  private val userPermissions =
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


  override def authenticate(
    req: RequestHeader
  )(
    implicit ec: ExecutionContext
  ): Future[Either[Result,UserPermissions]] = {

    log.warn(
      "Fake Authentication Service in operation! This always returns an authenticated user with full permissions, and is meant for testing purposes only!"
    )

    Future.successful(userPermissions.asRight[Result]) 

  }

  override  def setupPermissionModel(
    implicit ec: ExecutionContext
  ): Future[Boolean] =
    Future.successful(true)


}
