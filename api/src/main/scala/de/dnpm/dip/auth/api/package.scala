package de.dnpm.dip.auth
package object api {


import play.api.mvc.ActionFilter


// Helper type lambda 
private [api] type Authenticated[Agent] =
  ({ type Request[+T] = AuthenticatedRequest[Agent,T] })


type Authorization[Agent] =
  ActionFilter[Authenticated[Agent]#Request]

}
