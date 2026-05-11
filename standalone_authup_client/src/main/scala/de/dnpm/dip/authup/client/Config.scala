package de.dnpm.dip.authup.client


import java.io.{
  InputStream,
  FileInputStream
}
import scala.util.{
  Try,
  Failure,
  Properties
}
import scala.util.chaining._
import scala.util.Using
import scala.xml.XML
import de.dnpm.dip.util.Logging


final case class Credentials
(
  kind: SubjectKind.Value,
  id: String,
  secret: String
)

trait Config
{
  def baseUrl: String
  def credentials: Option[Credentials]
}


object Config extends Logging
{

  private final case class Impl
  (
    baseUrl: String,
    credentials: Option[Credentials]
  )
  extends Config


  private object Impl
  { 
   
    val urlRegex =
      raw"((client|robot|user):\/\/)?((\w+):(.+)@)?(.+)".r

    def from(url: String): Impl = {
      url match {
        case urlRegex(_,kind,_,id,secret,host) =>
          Impl(
            host,
            for {
              id     <- Option(id)
              secret <- Option(secret)
            } yield Credentials(
              Option(kind).map(SubjectKind.withName).getOrElse(SubjectKind.Client),
              id,
              secret
            )
          )

        case _ =>  
          s"Missing or ill-formatted Authup URL, expected format [[{user|robot|client}://]<id>:<secret>]@<host>"
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

    val urlEnv         = "AUTHUP_URL"
    val urlProp        = "dnpm.dip.authup.url"
    val configFileProp = "dnpm.dip.config.file"

    Try {
      Impl.from(Properties.envOrElse(urlEnv,Properties.propOrNull(urlProp)))
    }
    .recoverWith {
      case t =>
        log.warn(s"Couldn't load Authup client config from ENV variable '$urlEnv' or system property '$urlProp'",t)
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

}
