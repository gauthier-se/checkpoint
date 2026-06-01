import { Suspense, useState } from 'react'
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from '@tanstack/react-router'
import { useSuspenseQueries } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import type { ReviewCard as ReviewCardType } from '@/types/review'
import { GameGrid } from '@/components/games/game-grid'
import { CatalogFilters } from '@/components/games/catalog-filters'
import { DiscoverySection } from '@/components/games/discovery-section'
import { ReviewCard } from '@/components/reviews/review-card'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import {
  genresQueryOptions,
  mostBackloggedGamesQueryOptions,
  mostWishlistedGamesQueryOptions,
  platformsQueryOptions,
  trendingGamesQueryOptions,
} from '@/queries/catalog'
import {
  popularReviewsQueryOptions,
  recentReviewsQueryOptions,
} from '@/queries/review'

import { seo } from '@/lib/seo'
import {
  parseOptionalNumber,
  parseOptionalString,
  parseStringArray,
} from '@/lib/search-params'

const DISCOVERY_SIZE = 7

export type GamesSearchParams = {
  page?: number
}

export const Route = createFileRoute('/_app/games/')({
  head: () => ({
    meta: seo({ title: 'Games — Checkpoint' }),
  }),
  component: RouteComponent,
  pendingComponent: GamesIndexSkeleton,
  pendingMs: 0,
  validateSearch: (search: Record<string, unknown>): GamesSearchParams => ({
    page: parseOptionalNumber(search.page),
  }),
  beforeLoad: ({ search }) => {
    // Preserve backward-compatible deep links: any incoming filter or search
    // param re-routes to /games/filtered with the same values so existing
    // bookmarks keep working.
    const raw = search as Record<string, unknown>
    const forwardable: Record<string, unknown> = {}
    const stringKeys = ['q', 'sort'] as const
    const numberKeys = ['yearMin', 'yearMax', 'ratingMin', 'ratingMax'] as const
    let hasForwardableFilter = false
    for (const key of stringKeys) {
      const value = parseOptionalString(raw[key])
      if (value !== undefined) {
        forwardable[key] = value
        hasForwardableFilter = true
      }
    }
    // genre/platform accept both the new array form and the legacy singular
    // string form (old bookmarks like ?genre=RPG).
    const genres = parseStringArray(raw.genres ?? raw.genre)
    if (genres !== undefined) {
      forwardable.genres = genres
      hasForwardableFilter = true
    }
    const platforms = parseStringArray(raw.platforms ?? raw.platform)
    if (platforms !== undefined) {
      forwardable.platforms = platforms
      hasForwardableFilter = true
    }
    for (const key of numberKeys) {
      const value = parseOptionalNumber(raw[key])
      if (value !== undefined) {
        forwardable[key] = value
        hasForwardableFilter = true
      }
    }
    if (hasForwardableFilter) {
      forwardable.page = parseOptionalNumber(raw.page) ?? 1
      throw redirect({ to: '/games/filtered', search: forwardable })
    }
  },
  loader: ({ context }) => {
    void context.queryClient.prefetchQuery(trendingGamesQueryOptions())
    void context.queryClient.prefetchQuery(
      popularReviewsQueryOptions(DISCOVERY_SIZE),
    )
    void context.queryClient.prefetchQuery(
      recentReviewsQueryOptions(DISCOVERY_SIZE),
    )
    void context.queryClient.prefetchQuery(
      mostBackloggedGamesQueryOptions(DISCOVERY_SIZE),
    )
    void context.queryClient.prefetchQuery(
      mostWishlistedGamesQueryOptions(DISCOVERY_SIZE),
    )
    void context.queryClient.prefetchQuery(genresQueryOptions())
    void context.queryClient.prefetchQuery(platformsQueryOptions())
  },
})

function RouteComponent() {
  return (
    <Suspense fallback={<GamesIndexSkeleton />}>
      <GamesIndexContent />
    </Suspense>
  )
}

function GamesIndexContent() {
  const navigate = useNavigate({ from: '/games' })

  const [
    { data: trending },
    { data: popularReviews },
    { data: recentReviews },
    { data: mostBacklogged },
    { data: mostWishlisted },
  ] = useSuspenseQueries({
    queries: [
      trendingGamesQueryOptions(),
      popularReviewsQueryOptions(DISCOVERY_SIZE),
      recentReviewsQueryOptions(DISCOVERY_SIZE),
      mostBackloggedGamesQueryOptions(DISCOVERY_SIZE),
      mostWishlistedGamesQueryOptions(DISCOVERY_SIZE),
    ],
  })

  const [searchInput, setSearchInput] = useState('')

  function handleSearchKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && searchInput.trim().length > 0) {
      navigate({
        to: '/games/filtered',
        search: { page: 1, q: searchInput.trim() },
      })
    }
  }

  return (
    <div className="max-w-7xl mx-auto px-4">
      <div className="mt-10 py-2 text-muted-foreground font-semibold flex items-center justify-between gap-4 flex-wrap">
        <CatalogFilters search={{}} />
        <div className="flex items-center gap-4">
          <p className="min-w-fit">Find a game</p>
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Search..."
              className="pl-8"
            />
          </div>
        </div>
      </div>

      <DiscoverySection
        title="Popular games this week"
        action={
          <Link
            to="/games/filtered"
            search={{ page: 1, sort: 'rating,desc' }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            More
          </Link>
        }
      >
        {trending.length > 0 ? (
          <GameGrid games={trending} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No trending games yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Most backlogged">
        {mostBacklogged.length > 0 ? (
          <GameGrid games={mostBacklogged} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No games backlogged yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Most wishlisted">
        {mostWishlisted.length > 0 ? (
          <GameGrid games={mostWishlisted} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No games wishlisted yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Popular reviews">
        {popularReviews.length > 0 ? (
          <ReviewCardGrid reviews={popularReviews} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No popular reviews yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Just reviewed">
        {recentReviews.length > 0 ? (
          <ReviewCardGrid reviews={recentReviews} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No reviews yet.
          </p>
        )}
      </DiscoverySection>
    </div>
  )
}

function ReviewCardGrid({ reviews }: { reviews: Array<ReviewCardType> }) {
  return (
    <div className="grid grid-cols-1 gap-3 py-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {reviews.map((review) => (
        <ReviewCard key={review.id} review={review} />
      ))}
    </div>
  )
}

function GameCoversRowSkeleton() {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3 py-4">
      {Array.from({ length: 7 }).map((_, i) => (
        <Skeleton key={i} className="aspect-3/4 w-full rounded-sm" />
      ))}
    </div>
  )
}

function ReviewCardsRowSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-3 py-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-32 w-full rounded-md" />
      ))}
    </div>
  )
}

function GamesIndexSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4">
      <div className="mt-10 py-2 flex items-center justify-between gap-4 flex-wrap">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-8 w-48" />
      </div>
      {(
        [
          'Popular games this week',
          'Most backlogged',
          'Most wishlisted',
        ] as const
      ).map((title) => (
        <section key={title} className="my-8">
          <div className="flex items-center justify-between py-2">
            <Skeleton className="h-5 w-48" />
          </div>
          <Separator />
          <GameCoversRowSkeleton />
        </section>
      ))}
      {(['Popular reviews', 'Just reviewed'] as const).map((title) => (
        <section key={title} className="my-8">
          <div className="py-2">
            <Skeleton className="h-5 w-36" />
          </div>
          <Separator />
          <ReviewCardsRowSkeleton />
        </section>
      ))}
    </div>
  )
}
