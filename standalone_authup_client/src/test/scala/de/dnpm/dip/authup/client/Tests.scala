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

  System.setProperty("dnpm.dip.authup.baseurl","http://localhost:3001")
  System.setProperty("dnpm.dip.authup.admin.username","admin")
  System.setProperty("dnpm.dip.authup.admin.password","start123")


  private val service =
    StandaloneAuthupClient.instance
    

    
  "UserAuthentication SPI" must "have worked" in {

    UserAuthenticationService.getInstance.isSuccess mustBe true

  }


  "Authentication of a request without Bearer Token" must "of have failed" in {

     service.authenticate(FakeRequest()).map {
       case Left(result) => result.header.status mustBe 401
       case _            => fail()
     }

  }

  
  "Setting up permission model" must "have succeeded" in { 

    service.setupResult.map(_ mustBe true)

  }

}
