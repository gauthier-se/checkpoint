import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { DeleteAccountCard } from '@/components/settings/delete-account-card'
import { EditProfileForm } from '@/components/settings/edit-profile-form'
import { ExportDataCard } from '@/components/settings/export-data-card'
import { useAuth } from '@/hooks/use-auth'
import { userProfileQueryOptions } from '@/queries/profile'

export const Route = createFileRoute('/_app/_protected/settings/profile')({
  loader: async ({ context }) => {
    const user = await context.queryClient.ensureQueryData({
      queryKey: ['auth', 'me'],
      queryFn: async () => {
        const { apiFetch } = await import('@/services/api')
        const res = await apiFetch('/api/auth/me')
        if (!res.ok) return null
        return res.json()
      },
    })

    if (user) {
      await context.queryClient.ensureQueryData(
        userProfileQueryOptions(user.username),
      )
    }
  },
  component: SettingsProfilePage,
})

function SettingsProfilePage() {
  const { user } = useAuth()

  if (!user) return null

  const { data: profile } = useSuspenseQuery(
    userProfileQueryOptions(user.username),
  )

  return (
    <div className="space-y-6">
      <EditProfileForm
        username={profile.username}
        bio={profile.bio}
        picture={profile.picture}
        isPrivate={profile.isPrivate}
      />
      <ExportDataCard />
      <DeleteAccountCard username={profile.username} />
    </div>
  )
}
