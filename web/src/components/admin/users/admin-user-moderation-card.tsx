import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AdminUserDetail, AdminUserEdit } from '@/types/admin'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import { BanUserButton } from '@/components/admin/users/ban-user-button'
import { adminUsersQueryKey, editAdminUser } from '@/queries/admin/users'

/**
 * The moderation actions `PUT /admin/users/{id}` actually supports: clearing
 * the two free-text profile fields and forcing an account private. The endpoint
 * exposes no way to change a username, an email or a role, so those are not
 * offered here.
 */
export function AdminUserModerationCard({ user }: { user: AdminUserDetail }) {
  const queryClient = useQueryClient()

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: adminUsersQueryKey })

  const edit = (payload: AdminUserEdit) => editAdminUser(user.id, payload)

  const privacyMutation = useMutation({
    mutationFn: (isPrivate: boolean) => edit({ isPrivate }),
    onSuccess: async (updated) => {
      toast.success(
        updated.isPrivate
          ? `${user.username}'s profile is now private`
          : `${user.username}'s profile is now public`,
      )
      await invalidate()
    },
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Moderation</CardTitle>
        <CardDescription>
          Actions here take effect immediately and are recorded in the API logs.
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <Label htmlFor="admin-user-private">Private profile</Label>
            <p className="text-sm text-muted-foreground">
              Hides the profile and its activity from other members.
            </p>
          </div>
          <Switch
            id="admin-user-private"
            checked={user.isPrivate === true}
            disabled={privacyMutation.isPending}
            onCheckedChange={(checked) => privacyMutation.mutate(checked)}
          />
        </div>

        <div className="flex flex-wrap items-center gap-2 border-t pt-4">
          <AdminConfirmButton
            label="Clear bio"
            variant="outline"
            disabled={user.bio === null}
            disabledReason="This account has no bio"
            title={`Clear ${user.username}'s bio?`}
            description="The bio is deleted permanently. This cannot be undone."
            mutationFn={() => edit({ clearBio: true })}
            onSuccess={async () => {
              toast.success('Bio cleared')
              await invalidate()
            }}
          />

          <AdminConfirmButton
            label="Clear picture"
            variant="outline"
            disabled={user.picture === null}
            disabledReason="This account has no picture"
            title={`Clear ${user.username}'s picture?`}
            description="The profile picture is deleted permanently. This cannot be undone."
            mutationFn={() => edit({ clearPicture: true })}
            onSuccess={async () => {
              toast.success('Picture cleared')
              await invalidate()
            }}
          />

          <div className="ms-auto">
            <BanUserButton user={user} size="default" />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
