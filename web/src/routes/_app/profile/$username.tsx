import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { ProfileInlineTab } from '@/components/profile/profile-tab-bar'
import {
  userBacklogQueryOptions,
  userFollowingQueryOptions,
  userJournalQueryOptions,
  userLibraryQueryOptions,
  userLikedGamesQueryOptions,
  userProfileQueryOptions,
  userReviewsQueryOptions,
  userWishlistQueryOptions,
} from '@/queries/profile'
import { userTagGamesQueryOptions, userTagsQueryOptions } from '@/queries/tags'
import { ProfileHeader } from '@/components/profile/profile-header'
import { ProfileReviewsTab } from '@/components/profile/profile-reviews-tab'
import { ProfileWishlistTab } from '@/components/profile/profile-wishlist-tab'
import { ProfileFollowingTab } from '@/components/profile/profile-following-tab'
import { ProfileBacklogTab } from '@/components/profile/profile-backlog-tab'
import { ProfileJournalTab } from '@/components/profile/profile-journal-tab'
import { ProfileLikedTab } from '@/components/profile/profile-liked-tab'
import { ProfileTabBar } from '@/components/profile/profile-tab-bar'
import { TagsTab } from '@/components/collection/tags-tab'
import { BacklogTab, backlogQuery } from '@/components/collection/backlog-tab'
import { LikedTab, likedGamesQuery } from '@/components/collection/liked-tab'
import { PlayLogTab, playLogQuery } from '@/components/collection/play-log-tab'
import {
  WishlistTab,
  wishlistQuery,
} from '@/components/collection/wishlist-tab'
import { useAuth } from '@/hooks/use-auth'

// Search params

const VALID_INLINE_TABS: Array<ProfileInlineTab> = [
  'profile',
  'journal',
  'wishlist',
  'backlog',
  'tags',
  'liked',
  'reviews',
  'following',
]

type ProfileSearchParams = {
  tab: ProfileInlineTab
  page: number
  tagName?: string
}

// Route

export const Route = createFileRoute('/_app/profile/$username')({
  component: UserProfilePage,
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
  loader: async ({
    params: { username },
    context,
    deps: { tab, page, tagName },
  }) => {
    const profile = await context.queryClient.ensureQueryData(
      userProfileQueryOptions(username),
    )

    const apiPage = Math.max(0, page - 1)
    try {
      switch (tab) {
        case 'profile':
          // The Profile tab shows the recent-games preview inside ProfileHeader.
          void context.queryClient.prefetchQuery(
            userLibraryQueryOptions(username, 0, 8),
          )
          break
        case 'wishlist':
          if (profile.isOwner) {
            void context.queryClient.prefetchQuery(wishlistQuery(page))
          } else {
            void context.queryClient.prefetchQuery(
              userWishlistQueryOptions(username, apiPage),
            )
          }
          break
        case 'backlog':
          if (profile.isOwner) {
            void context.queryClient.prefetchQuery(backlogQuery(page))
          } else {
            void context.queryClient.prefetchQuery(
              userBacklogQueryOptions(username, apiPage),
            )
          }
          break
        case 'journal':
          if (profile.isOwner) {
            void context.queryClient.prefetchQuery(playLogQuery(page))
          } else {
            void context.queryClient.prefetchQuery(
              userJournalQueryOptions(username, apiPage),
            )
          }
          break
        case 'liked':
          if (profile.isOwner) {
            void context.queryClient.prefetchQuery(likedGamesQuery(page))
          } else {
            void context.queryClient.prefetchQuery(
              userLikedGamesQueryOptions(username, apiPage),
            )
          }
          break
        case 'tags':
          void context.queryClient.prefetchQuery(userTagsQueryOptions(username))
          if (tagName) {
            void context.queryClient.prefetchQuery(
              userTagGamesQueryOptions(username, tagName, apiPage),
            )
          }
          break
        case 'reviews':
          void context.queryClient.prefetchQuery(
            userReviewsQueryOptions(username, apiPage),
          )
          break
        case 'following':
          void context.queryClient.prefetchQuery(
            userFollowingQueryOptions(profile.id, apiPage),
          )
          break
      }
    } catch {
      // Silently ignore — tab component handles errors inline
    }

    return profile
  },
})

// Page

function UserProfilePage() {
  const { username } = Route.useParams()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))
  const { tab, page, tagName } = Route.useSearch()
  const { user } = useAuth()
  const isOwner = user?.username === profile.username

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileTabBar username={profile.username} activeTab={tab} />

      {tab === 'profile' && <ProfileHeader profile={profile} />}

      {tab === 'wishlist' &&
        (isOwner ? (
          <WishlistTab page={page} />
        ) : (
          <ProfileWishlistTab profile={profile} page={page} />
        ))}

      {tab === 'backlog' &&
        (isOwner ? (
          <BacklogTab page={page} />
        ) : (
          <ProfileBacklogTab profile={profile} page={page} />
        ))}

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

      {tab === 'liked' &&
        (isOwner ? (
          <LikedTab page={page} />
        ) : (
          <ProfileLikedTab profile={profile} page={page} />
        ))}

      {tab === 'reviews' && <ProfileReviewsTab profile={profile} page={page} />}

      {tab === 'following' && (
        <ProfileFollowingTab profile={profile} page={page} />
      )}
    </main>
  )
}
