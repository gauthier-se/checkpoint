import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { ProfileInlineTab } from '@/components/profile/profile-tab-bar'
import {
  userLibraryQueryOptions,
  userProfileQueryOptions,
  userReviewsQueryOptions,
} from '@/queries/profile'
import { userTagGamesQueryOptions, userTagsQueryOptions } from '@/queries/tags'
import { ProfileHeader } from '@/components/profile/profile-header'
import { ProfileReviewsTab } from '@/components/profile/profile-reviews-tab'
import { ProfileFollowersTab } from '@/components/profile/profile-followers-tab'
import { ProfileFollowingTab } from '@/components/profile/profile-following-tab'
import { ProfileJournalTab } from '@/components/profile/profile-journal-tab'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { ProfileSocialBar } from '@/components/profile/profile-social-bar'
import { TagsTab } from '@/components/collection/tags-tab'
import { PlayLogTab } from '@/components/collection/play-log-tab'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuth } from '@/hooks/use-auth'
import { seo } from '@/lib/seo'

// Search params

const VALID_INLINE_TABS: Array<ProfileInlineTab> = [
  'profile',
  'journal',
  'tags',
  'reviews',
  'followers',
  'following',
]

type ProfileSearchParams = {
  tab: ProfileInlineTab
  page: number
  tagName?: string
}

// Route

export const Route = createFileRoute('/_app/profile/$username')({
  component: RouteComponent,
  pendingComponent: UserProfileSkeleton,
  pendingMs: 0,
  head: ({ params }) => ({
    meta: seo({ title: `${params.username} — Checkpoint` }),
  }),
  validateSearch: (search: Record<string, unknown>): ProfileSearchParams => {
    const rawTab = String(search.tab ?? 'profile')
    const tab = VALID_INLINE_TABS.includes(rawTab as ProfileInlineTab)
      ? (rawTab as ProfileInlineTab)
      : 'profile'
    const page = Math.max(1, Math.floor(Number(search.page ?? 1)) || 1)
    const tagName =
      typeof search.tagName === 'string' && search.tagName.length > 0
        ? search.tagName
        : undefined
    return { tab, page, tagName }
  },
  loaderDeps: ({ search: { tab, page, tagName } }) => ({ tab, page, tagName }),
  loader: ({ params: { username }, context, deps: { tab, page, tagName } }) => {
    void context.queryClient.prefetchQuery(userProfileQueryOptions(username))
    const apiPage = Math.max(0, page - 1)
    switch (tab) {
      case 'profile':
        void context.queryClient.prefetchQuery(
          userLibraryQueryOptions(username, 0, 7),
        )
        break
      case 'reviews':
        void context.queryClient.prefetchQuery(
          userReviewsQueryOptions(username, apiPage),
        )
        break
      case 'tags':
        void context.queryClient.prefetchQuery(userTagsQueryOptions(username))
        if (tagName) {
          void context.queryClient.prefetchQuery(
            userTagGamesQueryOptions(username, tagName, apiPage),
          )
        }
        break
      // journal, followers, following: require profile data (isOwner / profile.id)
      // — loaded by the component after profile resolves
    }
  },
})

// Page

function RouteComponent() {
  return (
    <Suspense fallback={<UserProfileSkeleton />}>
      <UserProfilePage />
    </Suspense>
  )
}

function UserProfilePage() {
  const { username } = Route.useParams()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))
  const { tab, page, tagName } = Route.useSearch()
  const { user } = useAuth()
  const isOwner = user?.username === profile.username

  if (tab === 'profile') {
    return (
      <main className="mx-auto max-w-7xl px-4 py-10">
        <ProfileHeader
          profile={profile}
          nav={
            <ProfileTabBar
              username={profile.username}
              activeTab={tab}
              className="mb-0"
            />
          }
        />
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab={tab} />

      {(tab === 'followers' || tab === 'following') && (
        <ProfileSocialBar username={profile.username} activeTab={tab} />
      )}

      {tab === 'journal' &&
        (isOwner ? (
          <PlayLogTab page={page} />
        ) : (
          <ProfileJournalTab profile={profile} page={page} />
        ))}

      {tab === 'tags' && (
        <TagsTab
          username={profile.username}
          isOwner={isOwner}
          isPrivate={profile.isPrivate}
          selectedTag={tagName}
          page={page}
          tagLinkProps={(name) => ({
            to: '.',
            search: { tab: 'tags', tagName: name, page: 1 },
          })}
          pageLinkProps={(target) => ({
            to: '.',
            search: { tab: 'tags', tagName, page: target },
          })}
        />
      )}

      {tab === 'reviews' && <ProfileReviewsTab profile={profile} page={page} />}

      {tab === 'followers' && (
        <ProfileFollowersTab profile={profile} page={page} isOwner={isOwner} />
      )}

      {tab === 'following' && (
        <ProfileFollowingTab profile={profile} page={page} isOwner={isOwner} />
      )}
    </main>
  )
}

function UserProfileSkeleton() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <div className="space-y-8">
        {/* Hero: avatar + info */}
        <div className="flex flex-col gap-6 sm:flex-row sm:items-start">
          <Skeleton className="size-24 shrink-0 rounded-full" />
          <div className="flex-1 space-y-3">
            <Skeleton className="h-9 w-48" />
            <Skeleton className="h-4 w-64" />
            <Skeleton className="h-4 w-40" />
            <div className="flex gap-6">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-20" />
            </div>
            <Skeleton className="h-3 w-64" />
            <div className="flex gap-2 pt-1">
              <Skeleton className="h-9 w-28" />
            </div>
          </div>
        </div>

        {/* Body: main column + aside */}
        <div className="grid gap-8 xl:grid-cols-[1fr_360px]">
          <div className="min-w-0 space-y-8">
            {(['Favorite games', 'Recent activity'] as const).map((title) => (
              <div key={title} className="space-y-3">
                <div className="py-2">
                  <Skeleton className="h-5 w-36" />
                </div>
                <Separator />
                <div className="grid grid-cols-3 gap-3 py-2 sm:grid-cols-5">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton
                      key={i}
                      className="aspect-3/4 w-full rounded-sm"
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
          <aside className="space-y-8">
            <div className="space-y-3">
              <div className="py-2">
                <Skeleton className="h-5 w-16" />
              </div>
              <Separator />
              <div className="grid grid-cols-3 gap-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full rounded-md" />
                ))}
              </div>
            </div>
            <div className="space-y-3">
              <div className="py-2">
                <Skeleton className="h-5 w-40" />
              </div>
              <Separator />
              <Skeleton className="h-32 w-full rounded-md" />
            </div>
          </aside>
        </div>

        {/* Recent games — full-width row */}
        <div className="space-y-3">
          <div className="py-2">
            <Skeleton className="h-5 w-32" />
          </div>
          <Separator />
          <div className="grid grid-cols-3 gap-3 py-2 sm:grid-cols-5 md:grid-cols-7">
            {Array.from({ length: 7 }).map((_, i) => (
              <Skeleton key={i} className="aspect-3/4 w-full rounded-sm" />
            ))}
          </div>
        </div>
      </div>
    </main>
  )
}
