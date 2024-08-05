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


final case class CreatedPermission
(
  id: String,
  name: String
)

object CreatedPermission
{
  implicit val format: OFormat[CreatedPermission] =
    Json.format[CreatedPermission]
}


final case class CreatedRole
(
  id: String,
  name: String
)

object CreatedRole
{
  implicit val format: OFormat[CreatedRole] =
    Json.format[CreatedRole]
}


final case class Permission
(
  name: String,
  display_name: String,
  description: Option[String] = None
)

object Permission
{
  implicit val format: OFormat[Permission] =
    Json.format[Permission]
}

final case class Role
(
  name: String,
  display_name: String,
  description: Option[String] = None
)

object Role
{
  implicit val format: OFormat[Role] =
    Json.format[Role]
}

final case class RolePermission
(
  role_id: String,
  permission_id: String
)

object RolePermission
{
  implicit val format: OFormat[RolePermission] =
    Json.format[RolePermission]
}



final case class TokenIntrospection
(
  sub: String,
  sub_kind: SubKind.Value,
  exp: Long,
  name: String,
  given_name: Option[String],
  family_name: Option[String],
  permissions: Set[Permission]
)

object TokenIntrospection
{

  implicit val reads: Reads[TokenIntrospection] =
    Json.reads[TokenIntrospection]
}
