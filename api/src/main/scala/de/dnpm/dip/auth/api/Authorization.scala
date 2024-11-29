package de.dnpm.dip.auth.api


import scala.concurrent.{ExecutionContext,Future}
import play.api.mvc.{
  ActionFilter,
  ActionFunction,
  ActionBuilder,
  AnyContent,
  BaseController,
  BodyParser,
  ControllerHelpers,
  Result
}
import ControllerHelpers.Forbidden


/*

  Based on examples found in Play documentation on Action Composition:
 
  https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

  combined with inspiration drawn from Silhouette:

  https://github.com/mohiva/play-silhouette

*/

object Authorization
{

  def apply[Agent](
    check: Agent => Option[Result]
  )(
    implicit ec: ExecutionContext
  ): Authorization[Agent] =
    new ActionFilter[Authenticated[Agent]#Request]{

      override val executionContext = ec

      override def filter[T](request: AuthenticatedRequest[Agent,T]): Future[Option[Result]] =
        Future.successful(check(request.agent))
    }
 
  def async[Agent](
    check: Agent => Future[Option[Result]]
  )(
    implicit ec: ExecutionContext
  ): Authorization[Agent] =
    new ActionFilter[Authenticated[Agent]#Request]{

      override val executionContext = ec

      override def filter[T](request: AuthenticatedRequest[Agent,T]): Future[Option[Result]] =
        check(request.agent)
    }


  def check[Agent](
    check: Agent => Boolean,
    ifUnauthorized: => Result = Forbidden
  )(
    implicit ec: ExecutionContext
  ): Authorization[Agent] =
    Authorization[Agent](
      agent => check(agent) match {
        case true  => None
        case false => Some(ifUnauthorized)
      }
    )


  def async[Agent](
    check: Agent => Future[Boolean],
    ifUnauthorized: => Result = Forbidden
  )(
    implicit ec: ExecutionContext
  ): Authorization[Agent] =
    Authorization.async[Agent](
      check(_).map {
        case true  => None
        case false => Some(ifUnauthorized)
      }
    )

}


trait AuthorizationOps[Agent] extends AuthenticationOps[Agent]
{

  this: BaseController =>


  def AuthorizedAction[T](
    bodyParser: BodyParser[T]
  )(
    auth: Authorization[Agent],
//    auths: Authorization[Agent]*
    auths: ActionFunction[Authenticated[Agent]#Request,Authenticated[Agent]#Request]*
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,T] =
    AuthenticatedAction(bodyParser) andThen (
      (auth +: auths) reduce (_ andThen _)
    )

  def AuthorizedAction(
    auth: Authorization[Agent],
//    auths: Authorization[Agent]*
    auths: ActionFunction[Authenticated[Agent]#Request,Authenticated[Agent]#Request]*
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,AnyContent] =
    AuthenticatedAction andThen (
      (auth +: auths) reduce (_ andThen _)
    )

}


