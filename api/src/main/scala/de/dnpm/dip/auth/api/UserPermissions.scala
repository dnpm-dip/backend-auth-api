package de.dnpm.dip.auth.api



final case class UserPermissions
(
  id: String,
  username: String,
  givenName: String,
  familyName: String,
//  roles: Set[String],
  permissions: Set[String]
)
