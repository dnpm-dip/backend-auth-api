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

trait AuthenticationOps[Agent] extends AuthorizationOps[Agent]
{

  this: BaseController =>


  def AuthenticatedAction[T](
    bodyParser: BodyParser[T]
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,T] =
    new ActionBuilder[AuthReq,T]{

      override val parser =
        bodyParser

      override val executionContext =
        ec

      override def invokeBlock[A](
        request: Request[A],
        block: AuthReq[A] => Future[Result]
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


  def AuthenticatedAction[T](
    authorization: Authorization[Agent],
    bodyParser: BodyParser[T]
  )(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,T] =
    AuthenticatedAction(bodyParser) andThen Require(authorization)



  def AuthenticatedAction(
    implicit
    ec: ExecutionContext,
    authService: AuthenticationService[Agent]
  ): ActionBuilder[AuthReq,AnyContent] = 
    new ActionBuilder[AuthReq,AnyContent]{

      override val parser =
        controllerComponents.parsers.default

      override val executionContext =
        ec

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
  ): ActionBuilder[AuthReq,AnyContent] = 
    AuthenticatedAction andThen Require(authorization)

}

