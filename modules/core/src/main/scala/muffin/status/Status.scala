package muffin.status

import muffin.predef.*

trait Status[F[_]] {
  def getUserStatus(userId: UserId): F[UserStatus]

  def getUserStatuses(users: List[UserId]): F[List[UserId]]

  def updateUserStatus(userId: UserId, statusUser: StatusUser): F[Unit]

  def updateCustomStatus(userId: UserId, customStatus: CustomStatus): F[Unit]

  def unsetCustomStatus(userId: UserId): F[Unit]
}
