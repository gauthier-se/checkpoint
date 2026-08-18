import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import { useAuth } from '@/hooks/use-auth'
import { isBanned } from '@/lib/admin-users'
import {
  adminUsersQueryKey,
  banAdminUser,
  unbanAdminUser,
} from '@/queries/admin/users'

interface BanUserButtonProps {
  user: { id: string; username: string; banned: boolean | null }
  size?: 'sm' | 'default'
}

export function BanUserButton({ user, size = 'sm' }: BanUserButtonProps) {
  const queryClient = useQueryClient()
  const { user: currentUser } = useAuth()

  const banned = isBanned(user)
  // The API will happily ban the caller, which would lock the admin out of
  // their own panel on the next request. Block it in the UI.
  const isSelf = currentUser?.id === user.id

  return (
    <AdminConfirmButton
      label={banned ? 'Unban' : 'Ban'}
      variant={banned ? 'outline' : 'destructive'}
      size={size}
      disabled={isSelf}
      disabledReason="You cannot ban your own account"
      title={banned ? `Unban ${user.username}?` : `Ban ${user.username}?`}
      description={
        banned
          ? 'They will be able to sign in and post again.'
          : 'They will be blocked from signing in. Their existing content stays in place: remove it from the moderation queue if it also needs to go.'
      }
      mutationFn={() =>
        banned ? unbanAdminUser(user.id) : banAdminUser(user.id)
      }
      onSuccess={async () => {
        toast.success(
          banned
            ? `${user.username} was unbanned`
            : `${user.username} was banned`,
        )
        await queryClient.invalidateQueries({ queryKey: adminUsersQueryKey })
      }}
    />
  )
}
