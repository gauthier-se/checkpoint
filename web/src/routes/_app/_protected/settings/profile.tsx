import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { DeleteAccountCard } from '@/components/settings/delete-account-card'
import { EditProfileForm } from '@/components/settings/edit-profile-form'
import { ExportDataCard } from '@/components/settings/export-data-card'
import { FavoriteGamesEditor } from '@/components/settings/favorite-games-editor'
import { useAuth } from '@/hooks/use-auth'
import { userProfileQueryOptions } from '@/queries/profile'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/settings/profile')({
  head: () => ({
    meta: seo({ title: 'Profile settings — Checkpoint' }),
  }),
  loader: async ({ context }) => {
    const user = await context.queryClient.ensureQueryData({
      queryKey: ['auth', 'me'],
      queryFn: async () => {
        const { apiFetch, isApiError } = await import('@/services/api')
        try {
          const res = await apiFetch('/api/auth/me')
          return res.json()
        } catch (e) {
          if (isApiError(e) && (e.status === 401 || e.status === 403))
            return null
          throw e
        }
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
      <FavoriteGamesEditor
        username={profile.username}
        initialFavorites={profile.favorites}
      />
      <ExportDataCard />
      <DeleteAccountCard username={profile.username} />
    </div>
  )
}
