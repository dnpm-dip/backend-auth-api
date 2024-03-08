package de.dnpm.dip.auth.api



final case class UserPermissions
(
  id: String,
  username: String,
  givenName: Option[String],
  familyName: Option[String],
//  roles: Set[String],
  permissions: Set[String]
)
