package de.dnpm.dip.fake.auth


//import javax.inject.Inject
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import play.api.test.FakeRequest
import de.dnpm.dip.auth.api.UserAuthenticationService


//class Tests @Inject()(service: UserAuthenticationService) extends AsyncFlatSpec
class Tests extends AsyncFlatSpec
{

  val service =
    new FakeAuthService()


  "UserAuthentication SPI" must "have worked" in {

    UserAuthenticationService.getInstance.isSuccess mustBe true

  }


  "FakeAuthService.authenticate" must "have succeeded" in {

    service.authenticate(FakeRequest()).map(_.isRight mustBe true)

  }

}
