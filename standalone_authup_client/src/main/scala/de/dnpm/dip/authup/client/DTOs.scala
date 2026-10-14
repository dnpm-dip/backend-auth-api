package de.dnpm.dip.authup.client


import play.api.libs.json.{
  Json,
  Reads,
  Format,
  OFormat
}


object SubjectKind extends Enumeration
{
  val Client = Value("client")
  val User   = Value("user")

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
  displayName: Option[String],
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
  displayName: String,
  description: Option[String] = None
)

object Role
{
  implicit val format: OFormat[Role] =
    Json.format[Role]
}

final case class RolePermission
(
  roleId: String,
  permissionId: String
)

object RolePermission
{
  implicit val format: OFormat[RolePermission] =
    Json.format[RolePermission]
}


final case class AuthupResponse[T]
(
  data: T
)
object AuthupResponse
{
  implicit def reads[T: Reads]: Reads[AuthupResponse[T]] =
    Json.reads[AuthupResponse[T]]
}


final case class TokenIntrospection
(
  sub: String,
  sub_kind: SubjectKind.Value,
  active: Boolean,
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
