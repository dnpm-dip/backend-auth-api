package de.dnpm.dip.authup.client



import scala.util.Left
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import play.api.test.{
  FakeRequest,
}
import de.dnpm.dip.auth.api.UserAuthenticationService



/*
 *  NOTE:
 *  This is not a real/good test, as it requires
 *  an instance of the Authup backend running on localhost:3001
 */

class Tests extends AsyncFlatSpec
{

  System.setProperty("dnpm.dip.authup.url","robot://system:start123@http://localhost:3001")


  private val service =
    StandaloneAuthupClient.instance
    

    
  "UserAuthentication SPI" must "have worked" in {

    UserAuthenticationService.getInstance.isSuccess mustBe true

  }


  "Authentication of a request without Bearer Token" must "have failed" in {

     service.authenticate(FakeRequest()).map {
       case Left(result) => result.header.status mustBe 401
       case _            => fail()
     }

  }

  
  "Setting up permission model" must "have succeeded" in { 

    service.setupResult.map(_ mustBe true)

  }

}
