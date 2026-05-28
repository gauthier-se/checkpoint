import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { List } from 'lucide-react'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { userProfileQueryOptions } from '@/queries/profile'

// Placeholder route — the full implementation is tracked by TE-334.
export const Route = createFileRoute('/_app/profile_/$username/lists')({
  component: ProfileListsPage,
  loader: async ({ params: { username }, context }) => {
    return context.queryClient.ensureQueryData(
      userProfileQueryOptions(username),
    )
  },
})

function ProfileListsPage() {
  const { username } = Route.useParams()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab="lists" />
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <List className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          Lists page coming soon (TE-334).
        </p>
      </div>
    </main>
  )
}
