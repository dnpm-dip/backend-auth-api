package de.dnpm.dip.auth.api



final case class UserWithRoles
(
  id: String,
  username: String,
  givenName: String,
  familyName: String,
  permissions: Set[String]
)
