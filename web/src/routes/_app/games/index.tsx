import { useState } from 'react'
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import type { Game } from '@/types/game'
import type { ReviewCard as ReviewCardType } from '@/types/review'
import { GameGrid } from '@/components/games/game-grid'
import { CatalogFilters } from '@/components/games/catalog-filters'
import { DiscoverySection } from '@/components/games/discovery-section'
import { ReviewCard } from '@/components/reviews/review-card'
import { Input } from '@/components/ui/input'
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
const REVIEW_DISCOVERY_SIZE = 8

export type GamesSearchParams = {
  page?: number
}

export const Route = createFileRoute('/_app/games/')({
  head: () => ({
    meta: seo({ title: 'Games — Checkpoint' }),
  }),
  component: RouteComponent,
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
  loader: async ({
    context,
  }): Promise<{
    trending: Array<Game>
    popularReviews: Array<ReviewCardType>
    recentReviews: Array<ReviewCardType>
    mostBacklogged: Array<Game>
    mostWishlisted: Array<Game>
  }> => {
    const [
      trending,
      popularReviews,
      recentReviews,
      mostBacklogged,
      mostWishlisted,
    ] = await Promise.all([
      context.queryClient.ensureQueryData(trendingGamesQueryOptions()),
      context.queryClient.ensureQueryData(
        popularReviewsQueryOptions(REVIEW_DISCOVERY_SIZE),
      ),
      context.queryClient.ensureQueryData(
        recentReviewsQueryOptions(REVIEW_DISCOVERY_SIZE),
      ),
      context.queryClient.ensureQueryData(
        mostBackloggedGamesQueryOptions(DISCOVERY_SIZE),
      ),
      context.queryClient.ensureQueryData(
        mostWishlistedGamesQueryOptions(DISCOVERY_SIZE),
      ),
      context.queryClient.ensureQueryData(genresQueryOptions()),
      context.queryClient.ensureQueryData(platformsQueryOptions()),
    ])
    return {
      trending,
      popularReviews,
      recentReviews,
      mostBacklogged,
      mostWishlisted,
    }
  },
})

function RouteComponent() {
  const data = Route.useLoaderData()
  const navigate = useNavigate({ from: '/games' })

  // Keep loader-prefetched data live so likes/comments stay fresh on revisit.
  useQuery(popularReviewsQueryOptions(REVIEW_DISCOVERY_SIZE))
  useQuery(recentReviewsQueryOptions(REVIEW_DISCOVERY_SIZE))

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
    <div className="max-w-7xl mx-auto">
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
        {data.trending.length > 0 ? (
          <GameGrid games={data.trending} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No trending games yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Most backlogged">
        {data.mostBacklogged.length > 0 ? (
          <GameGrid games={data.mostBacklogged} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No games backlogged yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Most wishlisted">
        {data.mostWishlisted.length > 0 ? (
          <GameGrid games={data.mostWishlisted} columns={7} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No games wishlisted yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Popular reviews">
        {data.popularReviews.length > 0 ? (
          <ReviewCardGrid reviews={data.popularReviews} />
        ) : (
          <p className="py-8 text-center text-muted-foreground">
            No popular reviews yet.
          </p>
        )}
      </DiscoverySection>

      <DiscoverySection title="Just reviewed">
        {data.recentReviews.length > 0 ? (
          <ReviewCardGrid reviews={data.recentReviews} />
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
