package de.dnpm.dip.auth.api



import scala.concurrent.{ExecutionContext,Future}

import play.api.mvc.{
  AnyContent,
  ActionBuilder,
  ActionTransformer,
  BaseController,
  Request,
  Result,
  BodyParser
}


/*

  Based on examples found in Play documentation on Action Composition:
 
  https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

  combined with inspiration drawn from Silhouette:

  https://github.com/mohiva/play-silhouette

*/

trait AuthenticationOps[Agent] extends AuthorizationOps
{

  this: BaseController =>


  type AuthReq[+T] = AuthenticatedRequest[Agent,T]

  type AuthActionBuilder[Agent] = ActionBuilder[AuthReq,AnyContent]


  def AuthenticatedAction(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): AuthActionBuilder[Agent] =
    new ActionBuilder[AuthReq, AnyContent]{

      override val parser = controllerComponents.parsers.default

      override val executionContext = ec

      override def invokeBlock[T](
        request: Request[T],
        block: AuthReq[T] => Future[Result]
      ): Future[Result] =
        authService
          .authenticate(request)
          .flatMap {
            _.fold(
              Future.successful(_),  // forward the upstream auth provider's response
              agent => block(new AuthenticatedRequest(agent,request))
            )
          }
    }


  def AuthenticatedAction(
    authorization: Authorization[Agent]
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): AuthActionBuilder[Agent] = 
    AuthenticatedAction andThen Require(authorization)


}



trait RoleBasedAuthenticationOps[Agent,Role,Operation] extends AuthenticationOps[Agent]
{

  this: BaseController =>

  val rolesRights: RolesRightsMatrix[Role,Operation]


  def AuthenticatedOperation(
    op: Operation
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent],
    hasRoleSet: Agent <:< { def roles: Set[Role] }
  ): AuthActionBuilder[Agent] = 
    AuthenticatedAction andThen Require(rolesRights.authorizationFor(op))

}
