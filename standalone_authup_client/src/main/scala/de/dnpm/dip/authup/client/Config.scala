package de.dnpm.dip.authup.client


import scala.util.{
  Try,
  Failure
}
import de.dnpm.dip.util.Logging


import play.api.libs.json.{
  Json,
  OWrites
}

final case class Credentials
(
  username: String,
  password: String
)
object Credentials
{
  implicit val fromat: OWrites[Credentials] =
    Json.writes[Credentials]
}



trait Config
{
  def baseUrl: String
  def adminCredentials: Option[Credentials]
}


object Config extends Logging
{

  private final case class SysPropConfig
  (
    baseUrl: String,
    adminCredentials: Option[Credentials]
  )
  extends Config


  lazy val instance: Try[Config] = {

    val sysProp = "dnpm.dip.authup.baseurl"

    (
      for {
        baseUrl <-
          Try { Option(System.getProperty(sysProp)).get }
            .map(
              url =>
                if (url endsWith "/") url.substring(0,url.length-1)
                else url 
            )
      
        adminCredentials =
          for { 
            username <- Option(System.getProperty("dnpm.dip.authup.admin.username"))
            password <- Option(System.getProperty("dnpm.dip.authup.admin.password"))
          } yield Credentials(
            username,
            password
          )
        
      } yield SysPropConfig(
        baseUrl,
        adminCredentials
      )
    )
    .recoverWith {
      case t => 
        log.error(s"Couldn't initialize Authup client config: Undefined required system property $sysProp")
        Failure(t)
    }

  }

/*  
    Try { Option(System.getProperty(sysProp)).get }
      .map(
        url =>
          Impl(
            if (url endsWith "/") url.substring(0,url.length-1)
            else url 
          )
      )
      .recoverWith {
        case t => 
          log.error(s"Can't initialize Authup client config: Undefined system property $sysProp")
          Failure(t)
      }

  }
*/

}
