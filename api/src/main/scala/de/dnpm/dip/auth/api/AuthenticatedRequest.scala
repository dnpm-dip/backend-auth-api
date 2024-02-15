package de.dnpm.dip.auth.api



import play.api.mvc.{
  Request,
  WrappedRequest
}


/*

  Based on examples found in Play documentation on Action Composition:
 
  https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition#Action-composition

  combined with inspiration drawn from Silhouette:

  https://github.com/mohiva/play-silhouette

*/

class AuthenticatedRequest[Agent,+T](
  val agent: Agent,
  val request: Request[T]
)
extends WrappedRequest(request)


