package de.dnpm.dip.auth.api


import scala.concurrent.{ExecutionContext,Future}
import play.api.mvc.{
  ActionFilter,
  ActionBuilder,
  AnyContent,
  BaseController,
  BodyParser,
  ControllerHelpers,
  Result
}
import ControllerHelpers.{
  Unauthorized,
  Forbidden
}



/*

  Based on examples found in Play documentation on Action Composition:
 
  https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

  combined with inspiration drawn from Silhouette:

  https://github.com/mohiva/play-silhouette

*/

trait Authorization[Agent]
{
  self =>

  def isAuthorized(
    agent: Agent
  )(
    implicit ec: ExecutionContext
  ): Future[Boolean]


  def and(other: Authorization[Agent]): Authorization[Agent] =
    new Authorization[Agent]{
      override def isAuthorized(agent: Agent)(implicit ec: ExecutionContext) =
        self.isAuthorized(agent)
          .flatMap {
            // AND logic:
            // if first authorization check already fails, short-circuit to 'false'
            // else result depends on second auth check
            case false => Future.successful(false)
            case true  => other.isAuthorized(agent)  
          }
    }


  def or(other: Authorization[Agent]) =
    new Authorization[Agent]{
      override def isAuthorized(agent: Agent)(implicit ec: ExecutionContext) =
        self.isAuthorized(agent)
         .flatMap {
           // OR logic:
           // if first authorization check already succeeds, short-circuit to 'true'
           // else result depends on second auth check
           case true  => Future.successful(true)
           case false => other.isAuthorized(agent)
         }
    }
 
 
  def &&(other: Authorization[Agent]) = self and other
 
  def ||(other: Authorization[Agent]) = self or other
 
  def AND(other: Authorization[Agent]) = self and other
 
  def OR(other: Authorization[Agent]) = self or other
 
}


object Authorization
{

  def apply[Agent](f: Agent => Boolean): Authorization[Agent] =
    new Authorization[Agent]{
      override def isAuthorized(
        agent: Agent
      )(
        implicit ec: ExecutionContext
      ) = Future.successful(f(agent))
    }

  def async[Agent](f: Agent => Future[Boolean]): Authorization[Agent] =
    new Authorization[Agent]{
      override def isAuthorized(
        agent: Agent
      )(
        implicit ec: ExecutionContext
      ) = f(agent)
    }

/*  
  def ownerOf[Id,Agent](
    f: Id => Agent => Future[Boolean]
  ): Id => Authorization[Agent] =
    id => async { f(id) }
*/
}


trait AuthorizationOps[Agent] extends AuthenticationOps[Agent]
{

  this: BaseController =>


  def require(
    auth: Authorization[Agent],
    whenUnauthorized: => Result = Forbidden
  )(
    implicit ec: ExecutionContext
  ) = new ActionFilter[AuthReq]{

    override val executionContext = ec

    override def filter[T](request: AuthenticatedRequest[Agent,T]): Future[Option[Result]] = {
      auth.isAuthorized(request.agent)
        .map {
          case true  => None // No objections
          case false => Some(whenUnauthorized)
        }
    }

  }


  // Variation of above builder method require(..) with authorization argument, as extension method  
  implicit class AuthActionBuilderOps[T](actionBuilder: ActionBuilder[AuthReq,T])
  {
    def requiring(
      authorization: Authorization[Agent]
    )(
      implicit ec: ExecutionContext
    ) =
      actionBuilder andThen require(authorization)
  }


  def AuthorizedAction[T](
    bodyParser: BodyParser[T]
  )(
    authorization: Authorization[Agent]
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,T] =
    AuthenticatedAction(bodyParser) andThen require(authorization)


  def AuthorizedAction(
    authorization: Authorization[Agent]
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,AnyContent] =
    AuthenticatedAction andThen require(authorization)


}
