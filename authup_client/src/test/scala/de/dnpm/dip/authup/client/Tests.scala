package de.dnpm.dip.authup.client



import scala.util.Left
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import play.api.test.{
  FakeRequest,
  WsTestClient
}
import de.dnpm.dip.auth.api.UserAuthenticationService



class Tests extends AsyncFlatSpec
with WsTestClient
{

  System.setProperty("dnpm.dip.authup.baseurl","http://localhost:3001")
  System.setProperty("dnpm.dip.authup.admin.username","admin")
  System.setProperty("dnpm.dip.authup.admin.password","start123")

  private val client = 
    withClient(identity)

  private val service =
    new AuthupClient(client)
     

  "Authentication of a request with Bearer Token" must "of have failed" in {

     service.authenticate(FakeRequest()).map {
       case Left(result) => result.header.status mustBe 401
       case _            => fail()
     }

  }

  
  "Setting up permission model" must "have succeeded" in { 

    service.setupResult.map(_ mustBe true)
//    service.setupPermissionModel.map(_ mustBe true)

  }

}
