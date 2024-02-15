package de.dnpm.dip.authup.client


import play.api.libs.json.{
  Json,
  Reads,
  Format,
  OFormat
}


/*
   "active": true,
    "email": "peter.placzek1996@gmail.com",
    "email_verified": true,
    "exp": 1708006272,
    "family_name": null,
    "given_name": null,
    "iat": 1708002672,
    "iss": "https://dnpm.bwhealthcloud.de/auth/",
    "jti": "4717b463-37a2-46ae-9d4c-260373f56484",
    "kind": "access_token",
    "name": "admin",
    "nickname": "admin",
    "permissions": [
        {
            "condition": null,
            "inverse": false,
            "name": "client_add",
            "power": 999,
            "target": null
        },
        ...
    ]
    "preferred_username": "admin",
    "realm_id": "70bc90fc-e872-4cfe-80e6-d72a2dc2c42d",
    "realm_name": "master",
    "remote_address": "172.20.240.10",
    "scope": "global",
    "sub": "ad853d1a-34df-4848-b62d-2f1b577cc405",
    "sub_kind": "user",
    "updated_at": 1707994683000
*/


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


final case class TokenIntrospectionResult
(
  sub: String,
  sub_kind: SubKind.Value,
  exp: Long,
  name: String,
  given_name: String,
  family_name: String,
  permissions: Set[Permission]
)

object TokenIntrospectionResult
{

  implicit val reads: Reads[TokenIntrospectionResult] =
    Json.reads[TokenIntrospectionResult]
}
