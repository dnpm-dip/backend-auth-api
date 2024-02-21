package de.dnpm.dip.auth.api


import scala.concurrent.{ExecutionContext,Future}
import play.api.mvc.{
  ActionFilter,
  ControllerHelpers,
  Result
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

}


trait AuthorizationOps
{

  import ControllerHelpers.{
    Unauthorized,
    Forbidden
  }


  def Require[Agent](
    auth: Authorization[Agent],
    whenUnauthorized: => Result = Forbidden
  )(
    implicit ec: ExecutionContext
  ) = new ActionFilter[({ type Req[T] = AuthenticatedRequest[Agent,T] })#Req]{

    override val executionContext = ec

    override def filter[T](request: AuthenticatedRequest[Agent,T]): Future[Option[Result]] = {
      auth.isAuthorized(request.agent)
        .map {
          case true  => None // No objections
          case false => Some(whenUnauthorized)
        }
    }

  }

}

/*
object OwnerOf
{
  def resource[Agent,Id](id: Id, f: Agent => Id): Authorization[Agent] =
    Agent
}
*/
