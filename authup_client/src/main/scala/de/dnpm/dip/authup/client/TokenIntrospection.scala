package de.dnpm.dip.authup.client


import play.api.libs.json.{
  Json,
  Reads,
  Format,
  OFormat
}


object SubKind extends Enumeration
{
  val Client = Value("client")
  val User   = Value("user")
  val Robot  = Value("robot")

  implicit val format: Format[Value] =
    Json.formatEnum(this)
}


final case class Permission
(
  name: String
)

object Permission
{
  implicit val Format: OFormat[Permission] =
    Json.format[Permission]
}


final case class TokenIntrospection
(
  sub: String,
  sub_kind: SubKind.Value,
  exp: Long,
  name: String,
  given_name: String,
  family_name: String,
  permissions: Set[Permission]
)

object TokenIntrospection
{

  implicit val reads: Reads[TokenIntrospection] =
    Json.reads[TokenIntrospection]
}
