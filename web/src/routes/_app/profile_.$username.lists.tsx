import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ProfileListsTab } from '@/components/profile/profile-lists-tab'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { myListsQueryOptions, userListsQueryOptions } from '@/queries/lists'
import { userProfileQueryOptions } from '@/queries/profile'
import { authQueryOptions } from '@/hooks/use-auth'

type ProfileListsSearch = {
  page: number
}

export const Route = createFileRoute('/_app/profile_/$username/lists')({
  component: ProfileListsPage,
  validateSearch: (search: Record<string, unknown>): ProfileListsSearch => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search: { page } }) => ({ page }),
  loader: async ({ params: { username }, context, deps: { page } }) => {
    const profile = await context.queryClient.ensureQueryData(
      userProfileQueryOptions(username),
    )
    // Use auth (not profile.isOwner) to pick the prefetch — profile.isOwner is
    // stale during SSR when the cookie is unreachable. See TE-335.
    const user = await context.queryClient
      .ensureQueryData(authQueryOptions)
      .catch(() => null)
    const isOwner = user?.username === username
    const apiPage = Math.max(0, page - 1)
    void context.queryClient.prefetchQuery(
      isOwner
        ? myListsQueryOptions(apiPage)
        : userListsQueryOptions(username, apiPage),
    )
    return profile
  },
})

function ProfileListsPage() {
  const { username } = Route.useParams()
  const { page } = Route.useSearch()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab="lists" />
      <ProfileListsTab profile={profile} page={page} />
    </main>
  )
}
