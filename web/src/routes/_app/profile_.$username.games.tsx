import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { PlayStatus } from '@/types/interaction'
import type { ProfileGamesTabKey } from '@/components/profile/profile-tab-bar'
import type { LibrarySort } from '@/components/collection/library-tab'
import type { WishlistSort } from '@/components/collection/wishlist-tab'
import {
  LIBRARY_SORT_LABELS,
  LibraryTab,
  libraryQuery,
} from '@/components/collection/library-tab'
import { ProfileLibraryTab } from '@/components/profile/profile-library-tab'
import { ProfileAllGamesTab } from '@/components/profile/profile-all-games-tab'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { ProfileStatusBar } from '@/components/profile/profile-status-bar'
import { ProfileWishlistTab } from '@/components/profile/profile-wishlist-tab'
import { ProfileBacklogTab } from '@/components/profile/profile-backlog-tab'
import { ProfileLikedTab } from '@/components/profile/profile-liked-tab'
import { WishlistTab } from '@/components/collection/wishlist-tab'
import { BacklogTab } from '@/components/collection/backlog-tab'
import { LikedTab } from '@/components/collection/liked-tab'
import { SortSelect } from '@/components/collection/sort-select'
import {
  userAllGamesQueryOptions,
  userLibraryQueryOptions,
  userProfileQueryOptions,
} from '@/queries/profile'

import { seo } from '@/lib/seo'

const VALID_TABS: Array<ProfileGamesTabKey> = [
  'games',
  'playing',
  'played',
  'completed',
  'retired',
  'shelved',
  'abandoned',
  'wishlist',
  'backlog',
  'liked',
]

const VALID_SORTS: Array<LibrarySort> = [
  'rating',
  'addedAt',
  'updatedAt',
  'title',
]

const COLLECTION_SORT_LABELS: Record<WishlistSort, string> = {
  addedAt: 'Date added',
  priority: 'Priority',
}

const STATUS_FOR_TAB: Record<ProfileGamesTabKey, PlayStatus | undefined> = {
  games: undefined,
  playing: 'ARE_PLAYING',
  played: 'PLAYED',
  completed: 'COMPLETED',
  retired: 'RETIRED',
  shelved: 'SHELVED',
  abandoned: 'ABANDONED',
  wishlist: undefined,
  backlog: undefined,
  liked: undefined,
}

type ProfileGamesSearch = {
  tab: ProfileGamesTabKey
  page: number
  sort: LibrarySort
}

export const Route = createFileRoute('/_app/profile_/$username/games')({
  head: ({ params }) => ({
    meta: seo({ title: `${params.username}'s games — Checkpoint` }),
  }),
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
    if (tab === 'games') {
      void context.queryClient.prefetchQuery(
        userAllGamesQueryOptions(username, apiPage),
      )
    } else if (status !== undefined) {
      if (profile.isOwner) {
        void context.queryClient.prefetchQuery(libraryQuery(page, status, sort))
      } else {
        void context.queryClient.prefetchQuery(
          userLibraryQueryOptions(username, apiPage, 20, status),
        )
      }
    }
    // wishlist, backlog, liked: require profile data (isOwner / profile.id)
    // — prefetched in the component after profile resolves
    return profile
  },
})

function ProfileGamesPage() {
  const { username } = Route.useParams()
  const { tab, page, sort } = Route.useSearch()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))
  const navigate = Route.useNavigate()
  const [collectionSort, setCollectionSort] = useState<WishlistSort>('addedAt')

  const isStatusFilterTab =
    tab === 'playing' ||
    tab === 'played' ||
    tab === 'completed' ||
    tab === 'retired' ||
    tab === 'shelved' ||
    tab === 'abandoned'
  function onSortChange(newSort: LibrarySort) {
    void navigate({ search: { tab, page: 1, sort: newSort } })
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab={tab} />
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <ProfileStatusBar
          username={profile.username}
          activeTab={tab}
          sort={sort}
          className="mb-0"
        />
        {isStatusFilterTab && profile.isOwner && (
          <SortSelect
            value={sort}
            options={LIBRARY_SORT_LABELS}
            onChange={onSortChange}
          />
        )}
        {(tab === 'wishlist' || tab === 'backlog') && profile.isOwner && (
          <SortSelect
            value={collectionSort}
            options={COLLECTION_SORT_LABELS}
            onChange={setCollectionSort}
          />
        )}
      </div>

      {tab === 'games' && <ProfileAllGamesTab profile={profile} page={page} />}

      {isStatusFilterTab &&
        (profile.isOwner ? (
          <LibraryTab
            page={page}
            status={STATUS_FOR_TAB[tab]}
            sort={sort}
            tabKey={tab}
          />
        ) : (
          <ProfileLibraryTab
            profile={profile}
            page={page}
            status={STATUS_FOR_TAB[tab]}
            tabKey={tab}
          />
        ))}

      {tab === 'wishlist' &&
        (profile.isOwner ? (
          <WishlistTab page={page} sort={collectionSort} />
        ) : (
          <ProfileWishlistTab profile={profile} page={page} />
        ))}

      {tab === 'backlog' &&
        (profile.isOwner ? (
          <BacklogTab page={page} sort={collectionSort} />
        ) : (
          <ProfileBacklogTab profile={profile} page={page} />
        ))}

      {tab === 'liked' &&
        (profile.isOwner ? (
          <LikedTab page={page} />
        ) : (
          <ProfileLikedTab profile={profile} page={page} />
        ))}
    </main>
  )
}
