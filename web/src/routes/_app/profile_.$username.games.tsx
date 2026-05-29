import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { PlayStatus } from '@/types/interaction'
import type { ProfileGamesTabKey } from '@/components/profile/profile-tab-bar'
import type { LibrarySort } from '@/components/collection/library-tab'
import { LibraryTab, libraryQuery } from '@/components/collection/library-tab'
import { ProfileLibraryTab } from '@/components/profile/profile-library-tab'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import {
  userLibraryQueryOptions,
  userProfileQueryOptions,
} from '@/queries/profile'

const VALID_TABS: Array<ProfileGamesTabKey> = [
  'games',
  'playing',
  'played',
  'completed',
  'retired',
  'shelved',
  'abandoned',
]

const VALID_SORTS: Array<LibrarySort> = [
  'rating',
  'addedAt',
  'updatedAt',
  'title',
]

const STATUS_FOR_TAB: Record<ProfileGamesTabKey, PlayStatus | undefined> = {
  games: undefined,
  playing: 'ARE_PLAYING',
  played: 'PLAYED',
  completed: 'COMPLETED',
  retired: 'RETIRED',
  shelved: 'SHELVED',
  abandoned: 'ABANDONED',
}

type ProfileGamesSearch = {
  tab: ProfileGamesTabKey
  page: number
  sort: LibrarySort
}

export const Route = createFileRoute('/_app/profile_/$username/games')({
  component: ProfileGamesPage,
  validateSearch: (search: Record<string, unknown>): ProfileGamesSearch => {
    const rawTab = String(search.tab ?? 'games')
    const tab = VALID_TABS.includes(rawTab as ProfileGamesTabKey)
      ? (rawTab as ProfileGamesTabKey)
      : 'games'
    const page = Math.max(1, Math.floor(Number(search.page ?? 1)) || 1)
    const rawSort = String(search.sort ?? 'addedAt')
    const sort = VALID_SORTS.includes(rawSort as LibrarySort)
      ? (rawSort as LibrarySort)
      : 'addedAt'
    return { tab, page, sort }
  },
  loaderDeps: ({ search: { tab, page, sort } }) => ({ tab, page, sort }),
  loader: async ({
    params: { username },
    context,
    deps: { tab, page, sort },
  }) => {
    const profile = await context.queryClient.ensureQueryData(
      userProfileQueryOptions(username),
    )
    const apiPage = Math.max(0, page - 1)
    const status = STATUS_FOR_TAB[tab]
    if (profile.isOwner) {
      void context.queryClient.prefetchQuery(libraryQuery(page, status, sort))
    } else {
      void context.queryClient.prefetchQuery(
        userLibraryQueryOptions(username, apiPage, 20, status),
      )
    }
    return profile
  },
})

function ProfileGamesPage() {
  const { username } = Route.useParams()
  const { tab, page, sort } = Route.useSearch()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))
  const navigate = Route.useNavigate()

  function onSortChange(newSort: LibrarySort) {
    void navigate({ search: { tab, page: 1, sort: newSort } })
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab={tab} />

      {profile.isOwner ? (
        <LibraryTab
          page={page}
          status={STATUS_FOR_TAB[tab]}
          sort={sort}
          tabKey={tab}
          onSortChange={onSortChange}
        />
      ) : (
        <ProfileLibraryTab
          profile={profile}
          page={page}
          status={STATUS_FOR_TAB[tab]}
          tabKey={tab}
        />
      )}
    </main>
  )
}
