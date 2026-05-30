import { Link, createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { BadgeGrid } from '@/components/profile/badge-grid'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { userProfileQueryOptions } from '@/queries/profile'
import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/profile_/$username/badges')({
  component: ProfileBadgesPage,
  head: ({ params }) => ({
    meta: seo({ title: `${params.username}'s badges — Checkpoint` }),
  }),
  loader: async ({ params: { username }, context }) => {
    return context.queryClient.ensureQueryData(
      userProfileQueryOptions(username),
    )
  },
})

function ProfileBadgesPage() {
  const { username } = Route.useParams()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab="profile" />
      <div className="space-y-6">
        <Link
          to="/profile/$username"
          params={{ username: profile.username }}
          search={{ tab: 'profile', page: 1 }}
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm font-medium"
        >
          <ArrowLeft className="size-4" />
          Back to profile
        </Link>
        <h1 className="text-3xl font-bold">Badges</h1>
        <BadgeGrid badges={profile.badges} />
      </div>
    </main>
  )
}
