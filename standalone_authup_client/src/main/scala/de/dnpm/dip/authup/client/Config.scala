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


sealed abstract class Credentials

final case class UserCredentials
(
  username: String,
  password: String
)
extends Credentials

final case class RobotCredentials
(
  id: String,
  secret: String
)
extends Credentials

object Credentials
{

  implicit val writesUserCredentials: OWrites[UserCredentials] =
    Json.writes[UserCredentials]

  implicit val writesRobotCredentials: OWrites[RobotCredentials] =
    Json.writes[RobotCredentials]

  implicit val writes: OWrites[Credentials] =
    OWrites {
      case cr: RobotCredentials => Json.toJsObject(cr) 
      case cr: UserCredentials  => Json.toJsObject(cr) 
    }

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


  private object Impl
  { 
   
    val urlRegex =
      raw"((user|robot):\/\/)?((\w+):(.+)@)?(.+)".r

    def from(url: String): Impl = {
      url match {
        case urlRegex(_,typ,_,id,secret,host) =>
          Impl(
            host,
            for { 
              typ    <- Option(typ).orElse(Some("robot"))
              id     <- Option(id)
              secret <- Option(secret)
            } yield { 
              typ match {
                case "robot" => RobotCredentials(id,secret)
                case "user"  => UserCredentials(id,secret)
              }
            } 
          )

        case _ =>  
          s"Missing or ill-formated Authup URL '$url', expected format [[{user|robot}://]<id>:<secret>]@<host>"
            .tap(log.error)
            .pipe(msg => throw new IllegalArgumentException(msg))
      }
    }

  /*
   * Expected XML Config structure:
   *
   * <?xml version="1.0" encoding="UTF-8"?>
   * <Config>
   *   ...
   *   <Authup "url"="<id>:<secret>@<host>">
   *   ...
   * </Config>
   */
  
    def fromXML(in: InputStream): Config = 
      Impl.from(XML.load(in) \\ "Authup" \@ "url")

  }
  


  lazy val instance: Try[Config] = {

    val urlProp        = "dnpm.dip.authup.url"
    val configFileProp = "dnpm.dip.config.file"

    Try {
      Impl.from(System.getProperty(urlProp))
    }
    .recoverWith {
      case t =>
        log.warn(s"Couldn't load Authup client config from system property '$urlProp'",t)
        log.warn(s"Attempting fallback setup path via config file '$configFileProp'")
    
        Using(new FileInputStream(System.getProperty(configFileProp)))(
          Impl.fromXML
        )
    }
    .recoverWith {
      case t => 
        log.error(s"Couldn't load Authup client config",t)
        Failure(t)
    }

  }

/*
  lazy val instance: Try[Config] = {

    val configFileProp =
      "dnpm.dip.config.file"

    Using(new FileInputStream(System.getProperty(configFileProp)))(
      Impl.fromXML
    )
    .recoverWith {
      case t =>
        val urlProp = "dnpm.dip.authup.url"

        log.warn(s"Couldn't load Authup client config, most likely due to undefined System property $configFileProp",t)
        log.warn(s"Attempting fallback setup path via system property $urlProp")

        Try {
          Option(System.getProperty(urlProp)).get
        }
        .map(Impl.from)
    }
    .recoverWith {
      case t => 
        log.error(s"Couldn't load Authup client config",t)
        Failure(t)
    }

  }
*/
}
