package de.dnpm.dip.authup.client


import java.io.{
  InputStream,
  FileInputStream
}
import scala.util.{
  Try,
  Failure
}
import scala.util.chaining._
import scala.util.Using
import scala.xml.XML
import play.api.libs.json.{
  Json,
  OWrites
}
import de.dnpm.dip.util.Logging



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

  private final case class Impl
  (
    baseUrl: String,
    adminCredentials: Option[Credentials]
  )
  extends Config

 /*
   * Expected XML Config structure:
   *
   * <?xml version="1.0" encoding="UTF-8"?>
   * <Config>
   *   ...
   *   <Authup>
   *     <baseURL value="http://localhost"/>
   *    
   *     <!-- OPTIONAL: Admin credential for auto-setup of permission/role model -->
   *     <Admin username="admin" password="TOP_SECRET"/>
   *   </Authup>
   *   ...
   * </Config>
   */
  
  private def parseXML(in: InputStream): Config = {

    val xml =
      (XML.load(in) \\ "Authup")

    Impl(
      (xml \ "baseURL" \@ "value"),
      Option(
        (xml \ "Admin" \@ "username") -> (xml \ "Admin" \@ "password")
      )
      .collect {
        case (username,password) if !(username.isBlank || password.isBlank) =>
          Credentials(username,password)
      }
    )

  }


  lazy val instance: Try[Config] = {

    val configFileProp = "dnpm.dip.config.file"

    Using(new FileInputStream(System.getProperty(configFileProp)))(
      parseXML
    )
    .recoverWith {
      case t =>
        log.warn(s"Couldn't load Authup client config, most likely due to undefined System property $configFileProp",t)
        log.warn(s"Attempting fallback path via system properties")

        val baseUrlProp = "dnpm.dip.authup.baseurl"
        
        for {
          baseUrl <-
            Try { Option(System.getProperty(baseUrlProp)).get }
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
          
        } yield Impl(
          baseUrl,
          adminCredentials
        )
    }
    .recoverWith {
      case t => 
        log.error(s"Couldn't load Authup client config",t)
        Failure(t)
    }

  }

}
