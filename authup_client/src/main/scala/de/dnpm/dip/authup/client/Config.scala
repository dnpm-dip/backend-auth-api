package de.dnpm.dip.authup.client


import scala.util.{
  Try,
  Failure
}
import de.dnpm.dip.util.Logging


trait Config
{
  def baseUrl: String
}


object Config extends Logging
{

  private final case class Impl
  (
    baseUrl: String
  )
  extends Config


  lazy val instance: Try[Config] = {

    val sysProp = "dnpm.dip.authup.baseurl"
    
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


}
