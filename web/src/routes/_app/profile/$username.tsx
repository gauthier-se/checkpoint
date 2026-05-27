import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import {
  Archive,
  BookOpen,
  Heart,
  Library,
  List,
  MessageSquare,
  Tag,
  ThumbsUp,
  Users,
} from 'lucide-react'
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
import { userListsQueryOptions } from '@/queries/lists'
import { userTagGamesQueryOptions, userTagsQueryOptions } from '@/queries/tags'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ProfileHeader } from '@/components/profile/profile-header'
import { ProfileReviewsTab } from '@/components/profile/profile-reviews-tab'
import { ProfileWishlistTab } from '@/components/profile/profile-wishlist-tab'
import { ProfileFollowingTab } from '@/components/profile/profile-following-tab'
import { ProfileListsTab } from '@/components/profile/profile-lists-tab'
import { ProfileLibraryTab } from '@/components/profile/profile-library-tab'
import { ProfileBacklogTab } from '@/components/profile/profile-backlog-tab'
import { ProfileJournalTab } from '@/components/profile/profile-journal-tab'
import { ProfileLikedTab } from '@/components/profile/profile-liked-tab'
import { TagsTab } from '@/components/collection/tags-tab'

// Search params

type ProfileTab =
  | 'library'
  | 'wishlist'
  | 'backlog'
  | 'journal'
  | 'tags'
  | 'liked'
  | 'reviews'
  | 'lists'
  | 'following'

const VALID_TABS: Array<ProfileTab> = [
  'library',
  'wishlist',
  'backlog',
  'journal',
  'tags',
  'liked',
  'reviews',
  'lists',
  'following',
]

type ProfileSearchParams = {
  tab: ProfileTab
  page: number
  tagName?: string
}

// Route

export const Route = createFileRoute('/_app/profile/$username')({
  component: UserProfilePage,
  validateSearch: (search: Record<string, unknown>): ProfileSearchParams => {
    const rawTab = String(search.tab ?? 'library')
    const tab = VALID_TABS.includes(rawTab as ProfileTab)
      ? (rawTab as ProfileTab)
      : 'library'
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

    // Prefetch active tab data
    const apiPage = Math.max(0, page - 1)
    try {
      switch (tab) {
        case 'library':
          void context.queryClient.prefetchQuery(
            userLibraryQueryOptions(username, apiPage),
          )
          break
        case 'wishlist':
          void context.queryClient.prefetchQuery(
            userWishlistQueryOptions(username, apiPage),
          )
          break
        case 'backlog':
          void context.queryClient.prefetchQuery(
            userBacklogQueryOptions(username, apiPage),
          )
          break
        case 'journal':
          void context.queryClient.prefetchQuery(
            userJournalQueryOptions(username, apiPage),
          )
          break
        case 'liked':
          void context.queryClient.prefetchQuery(
            userLikedGamesQueryOptions(username, apiPage),
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
        case 'reviews':
          void context.queryClient.prefetchQuery(
            userReviewsQueryOptions(username, apiPage),
          )
          break
        case 'lists':
          void context.queryClient.prefetchQuery(
            userListsQueryOptions(username, apiPage),
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

// Tab config

const TAB_CONFIG: Array<{
  value: ProfileTab
  label: string
  icon: React.ReactNode
}> = [
  { value: 'library', label: 'Library', icon: <Library className="size-4" /> },
  { value: 'wishlist', label: 'Wishlist', icon: <Heart className="size-4" /> },
  { value: 'backlog', label: 'Backlog', icon: <Archive className="size-4" /> },
  { value: 'journal', label: 'Journal', icon: <BookOpen className="size-4" /> },
  { value: 'tags', label: 'Tags', icon: <Tag className="size-4" /> },
  { value: 'liked', label: 'Liked', icon: <ThumbsUp className="size-4" /> },
  {
    value: 'reviews',
    label: 'Reviews',
    icon: <MessageSquare className="size-4" />,
  },
  { value: 'lists', label: 'Lists', icon: <List className="size-4" /> },
  {
    value: 'following',
    label: 'Following',
    icon: <Users className="size-4" />,
  },
]

// Page

function UserProfilePage() {
  const { username } = Route.useParams()
  const { data: profile } = useSuspenseQuery(userProfileQueryOptions(username))
  const { tab, page, tagName } = Route.useSearch()
  const navigate = Route.useNavigate()

  function onTabChange(newTab: string) {
    void navigate({
      search: { tab: newTab as ProfileTab, page: 1 },
    })
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <ProfileHeader profile={profile} />

      <Tabs value={tab} onValueChange={onTabChange} className="mt-6">
        <TabsList variant="line" className="mb-6 w-full justify-start">
          {TAB_CONFIG.map(({ value, label, icon }) => (
            <TabsTrigger key={value} value={value} className="gap-2 px-4 py-2">
              {icon}
              {label}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="library">
          <ProfileLibraryTab
            profile={profile}
            page={tab === 'library' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="wishlist">
          <ProfileWishlistTab
            profile={profile}
            page={tab === 'wishlist' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="backlog">
          <ProfileBacklogTab
            profile={profile}
            page={tab === 'backlog' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="journal">
          <ProfileJournalTab
            profile={profile}
            page={tab === 'journal' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="tags">
          <TagsTab
            username={profile.username}
            isOwner={profile.isOwner}
            isPrivate={profile.isPrivate}
            selectedTag={tab === 'tags' ? tagName : undefined}
            page={tab === 'tags' ? page : 1}
            tagLinkProps={(name) => ({
              to: '.',
              search: { tab: 'tags', tagName: name, page: 1 },
            })}
            pageLinkProps={(target) => ({
              to: '.',
              search: { tab: 'tags', tagName, page: target },
            })}
          />
        </TabsContent>

        <TabsContent value="liked">
          <ProfileLikedTab
            profile={profile}
            page={tab === 'liked' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="reviews">
          <ProfileReviewsTab
            profile={profile}
            page={tab === 'reviews' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="lists">
          <ProfileListsTab
            profile={profile}
            page={tab === 'lists' ? page : 1}
          />
        </TabsContent>

        <TabsContent value="following">
          <ProfileFollowingTab
            profile={profile}
            page={tab === 'following' ? page : 1}
          />
        </TabsContent>
      </Tabs>
    </main>
  )
}
