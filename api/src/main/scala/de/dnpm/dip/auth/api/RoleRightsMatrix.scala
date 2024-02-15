package de.dnpm.dip.auth.api



class RolesRightsMatrix[Role,Operation] private (
  private val rows: Map[Role,Set[Operation]]
)
{

  def authorizationFor[Agent](
    op: Operation
  )(
    implicit hasRoles: Agent <:< { def roles: Set[Role] }
  ): Authorization[Agent] = {

    import scala.language.reflectiveCalls

    Authorization[Agent]{ agent =>
      rows.exists {
        case (role,ops) => (agent.roles contains role) && (ops contains op)
      }
    }

  } 

}


object RolesRightsMatrix
{

  def apply[Role,Operation](
    row:  (Role,Set[Operation]),
    rows: (Role,Set[Operation])*
  ): RolesRightsMatrix[Role,Operation] =
    new RolesRightsMatrix(
      (row +: rows).toMap
    )

}
