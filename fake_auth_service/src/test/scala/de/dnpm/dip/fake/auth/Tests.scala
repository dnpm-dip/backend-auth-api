package de.dnpm.dip.fake.auth


import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.EitherValues._
import play.api.test.FakeRequest
import de.dnpm.dip.auth.api.UserAuthenticationService


class Tests extends AsyncFlatSpec
{

  val service =
    new FakeAuthService()


  "UserAuthentication SPI" must "have worked" in {

    UserAuthenticationService.getInstance.isSuccess mustBe true

  }


  "Dummy Authentication" must "have succeeded" in {

    for {
      result <- service.authenticate(FakeRequest())
    } yield result.isRight mustBe true

  }

  "Dummy user" must "have non-empty set of permissions" in {

    for {
      result <- service.authenticate(FakeRequest())

      user = result.value

    } yield user.permissions must not be (empty) 

  }

}
