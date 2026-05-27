import { createFileRoute } from '@tanstack/react-router'
import { Archive, BookOpen, Heart, Library, Tag, ThumbsUp } from 'lucide-react'
import type { CollectionTab } from '@/types/collection'
import { BacklogTab, backlogQuery } from '@/components/collection/backlog-tab'
import { LibraryTab, libraryQuery } from '@/components/collection/library-tab'
import { LikedTab, likedGamesQuery } from '@/components/collection/liked-tab'
import { PlayLogTab, playLogQuery } from '@/components/collection/play-log-tab'
import { TagsTab } from '@/components/collection/tags-tab'
import {
  WishlistTab,
  wishlistQuery,
} from '@/components/collection/wishlist-tab'
import { userTagGamesQueryOptions, userTagsQueryOptions } from '@/queries/tags'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

// Search params

const VALID_TABS: Array<CollectionTab> = [
  'library',
  'wishlist',
  'backlog',
  'journal',
  'tags',
  'liked',
]

type GamesSearchParams = {
  tab: CollectionTab
  page: number
  tagName?: string
}

// Route

export const Route = createFileRoute('/_app/_protected/$username/games/')({
  component: UserGamesPage,
  validateSearch: (search: Record<string, unknown>): GamesSearchParams => {
    const rawTab = String(search.tab ?? 'library')
    const tab = VALID_TABS.includes(rawTab as CollectionTab)
      ? (rawTab as CollectionTab)
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
    context,
    params: { username },
    deps: { tab, page, tagName },
  }) => {
    // Prefetch data for the active tab; wrapped in try/catch so
    // missing API endpoints (not yet merged) don't crash navigation
    try {
      switch (tab) {
        case 'library':
          await context.queryClient.ensureQueryData(libraryQuery(page))
          break
        case 'wishlist':
          await context.queryClient.ensureQueryData(wishlistQuery(page))
          break
        case 'backlog':
          await context.queryClient.ensureQueryData(backlogQuery(page))
          break
        case 'journal':
          await context.queryClient.ensureQueryData(playLogQuery(page))
          break
        case 'liked':
          await context.queryClient.ensureQueryData(likedGamesQuery(page))
          break
        case 'tags':
          await context.queryClient.ensureQueryData(
            userTagsQueryOptions(username),
          )
          if (tagName) {
            void context.queryClient.prefetchQuery(
              userTagGamesQueryOptions(
                username,
                tagName,
                Math.max(0, page - 1),
              ),
            )
          }
          break
      }
    } catch {
      // Silently ignore — the tab component will show the error inline
    }
  },
})

// Tab config

const TAB_CONFIG: Array<{
  value: CollectionTab
  label: string
  icon: React.ReactNode
}> = [
  { value: 'library', label: 'Library', icon: <Library className="size-4" /> },
  { value: 'wishlist', label: 'Wishlist', icon: <Heart className="size-4" /> },
  { value: 'backlog', label: 'Backlog', icon: <Archive className="size-4" /> },
  { value: 'journal', label: 'Journal', icon: <BookOpen className="size-4" /> },
  { value: 'tags', label: 'Tags', icon: <Tag className="size-4" /> },
  { value: 'liked', label: 'Liked', icon: <ThumbsUp className="size-4" /> },
]

// Page

function UserGamesPage() {
  const { username } = Route.useParams()
  const { tab, page, tagName } = Route.useSearch()
  const navigate = Route.useNavigate()

  function onTabChange(newTab: string) {
    void navigate({
      search: { tab: newTab as CollectionTab, page: 1 },
    })
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="mb-8 text-3xl font-bold">My Games</h1>

      <Tabs value={tab} onValueChange={onTabChange}>
        <TabsList variant="line" className="mb-6 w-full justify-start">
          {TAB_CONFIG.map(({ value, label, icon }) => (
            <TabsTrigger key={value} value={value} className="gap-2 px-4 py-2">
              {icon}
              {label}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="library">
          <LibraryTab page={tab === 'library' ? page : 1} />
        </TabsContent>

        <TabsContent value="wishlist">
          <WishlistTab page={tab === 'wishlist' ? page : 1} />
        </TabsContent>

        <TabsContent value="backlog">
          <BacklogTab page={tab === 'backlog' ? page : 1} />
        </TabsContent>

        <TabsContent value="journal">
          <PlayLogTab page={tab === 'journal' ? page : 1} />
        </TabsContent>

        <TabsContent value="tags">
          <TagsTab
            username={username}
            isOwner
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
          <LikedTab page={tab === 'liked' ? page : 1} />
        </TabsContent>
      </Tabs>
    </main>
  )
}
