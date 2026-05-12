import { useDeferredValue, useEffect, useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { queryOptions, useQuery } from '@tanstack/react-query'
import { Loader2, Search, X } from 'lucide-react'
import type { Game, GamesResponse } from '@/types/game'
import { GameGrid } from '@/components/games/game-grid'
import { CatalogFilters } from '@/components/games/catalog-filters'
import { GamesPagination } from '@/components/games/pagination'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { apiFetch } from '@/services/api'
import {
  genresQueryOptions,
  platformsQueryOptions,
  trendingGamesQueryOptions,
} from '@/queries/catalog'

const PAGE_SIZE = 32

const VALID_SORTS = [
  'releaseDate,desc',
  'releaseDate,asc',
  'title,asc',
  'title,desc',
  'rating,desc',
  'rating,asc',
] as const

export type GamesSearchParams = {
  page: number
  q?: string
  genre?: string
  platform?: string
  yearMin?: number
  yearMax?: number
  ratingMin?: number
  ratingMax?: number
  sort?: string
}

function parseOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined
}

function parseOptionalNumber(value: unknown): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

function parseSort(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  return (VALID_SORTS as ReadonlyArray<string>).includes(value)
    ? value
    : undefined
}

function searchGamesQuery(q: string) {
  return queryOptions({
    queryKey: ['games', 'search', q],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch(`/api/games/search?q=${encodeURIComponent(q)}`)
      if (!res.ok) throw new Error('Failed to search games')
      return res.json()
    },
  })
}

function buildCatalogUrl(params: GamesSearchParams): string {
  const qs = new URLSearchParams()
  qs.set('page', String(params.page - 1))
  qs.set('size', String(PAGE_SIZE))
  qs.set('sort', params.sort ?? 'releaseDate,desc')
  if (params.genre) qs.set('genre', params.genre)
  if (params.platform) qs.set('platform', params.platform)
  if (params.yearMin != null) qs.set('yearMin', String(params.yearMin))
  if (params.yearMax != null) qs.set('yearMax', String(params.yearMax))
  if (params.ratingMin != null) qs.set('ratingMin', String(params.ratingMin))
  if (params.ratingMax != null) qs.set('ratingMax', String(params.ratingMax))
  return `/api/games?${qs.toString()}`
}

export const Route = createFileRoute('/_app/games/')({
  component: RouteComponent,
  validateSearch: (search: Record<string, unknown>): GamesSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseOptionalString(search.q),
    genre: parseOptionalString(search.genre),
    platform: parseOptionalString(search.platform),
    yearMin: parseOptionalNumber(search.yearMin),
    yearMax: parseOptionalNumber(search.yearMax),
    ratingMin: parseOptionalNumber(search.ratingMin),
    ratingMax: parseOptionalNumber(search.ratingMax),
    sort: parseSort(search.sort),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({
    deps,
    context,
  }): Promise<{ catalog: GamesResponse; trending: Array<Game> }> => {
    const [catalog, trending] = await Promise.all([
      apiFetch(buildCatalogUrl(deps)).then(
        (res): Promise<GamesResponse> => res.json(),
      ),
      context.queryClient.ensureQueryData(trendingGamesQueryOptions()),
      context.queryClient.ensureQueryData(genresQueryOptions()),
      context.queryClient.ensureQueryData(platformsQueryOptions()),
    ])
    return { catalog, trending }
  },
})

function RouteComponent() {
  const data = Route.useLoaderData()
  const search = Route.useSearch()
  const { page, q } = search
  const navigate = useNavigate({ from: '/games' })

  const [inputValue, setInputValue] = useState(q ?? '')
  const deferredQuery = useDeferredValue(inputValue)

  const isSearchActive = deferredQuery.length >= 2

  const {
    data: searchResults,
    isLoading: isSearchLoading,
    isFetching: isSearchFetching,
  } = useQuery({
    ...searchGamesQuery(deferredQuery),
    enabled: isSearchActive,
  })

  // Sync deferred query to URL
  useEffect(() => {
    const urlQ = deferredQuery.length >= 2 ? deferredQuery : undefined
    if (urlQ !== q) {
      navigate({
        search: (prev) => ({ ...prev, q: urlQ }),
        replace: true,
      })
    }
  }, [deferredQuery, q, navigate])

  // Sync URL q to input when navigating directly (e.g. shared link)
  useEffect(() => {
    if (q && q !== inputValue) {
      setInputValue(q)
    }
  }, [q])

  function clearSearch() {
    setInputValue('')
    navigate({
      search: (prev) => ({ ...prev, q: undefined }),
      replace: true,
    })
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10 py-2 text-muted-foreground font-semibold flex items-center justify-between">
        <CatalogFilters />
        <div className="flex items-center gap-4">
          <p className="min-w-fit">Find a game</p>
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Search..."
              className="pl-8 pr-8"
            />
            {inputValue.length > 0 && (
              <button
                type="button"
                onClick={clearSearch}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                <X className="size-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {isSearchActive ? (
        <div className="my-8">
          <h2 className="py-2 text-muted-foreground font-semibold">
            {isSearchLoading
              ? 'Searching...'
              : searchResults && searchResults.length > 0
                ? `${searchResults.length} result${searchResults.length > 1 ? 's' : ''} for "${deferredQuery}"`
                : `No games found for "${deferredQuery}"`}
          </h2>
          <Separator />
          {isSearchLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : searchResults && searchResults.length > 0 ? (
            <div className="relative">
              {isSearchFetching && (
                <div className="absolute inset-0 bg-background/50 flex justify-center pt-12 z-10">
                  <Loader2 className="size-6 animate-spin text-muted-foreground" />
                </div>
              )}
              <GameGrid games={searchResults} />
            </div>
          ) : (
            <p className="py-8 text-center text-muted-foreground">
              No games to display.
            </p>
          )}
        </div>
      ) : (
        <>
          <div className="my-8">
            <div className="py-2 text-muted-foreground font-semibold flex items-center justify-between">
              <h2>Popular games this week</h2>
              <Link to="/games" search={{ page: 1, sort: 'rating,desc' }}>
                More
              </Link>
            </div>
            <Separator />
            <GameGrid games={data.trending} columns={7} />
          </div>
          <h2 id="catalog" className="py-2 text-muted-foreground font-semibold">
            {data.catalog.metadata.totalElements === 0
              ? 'No games found'
              : `There ${data.catalog.metadata.totalElements > 1 ? 'are' : 'is'} ${data.catalog.metadata.totalElements} game${data.catalog.metadata.totalElements > 1 ? 's' : ''}`}
          </h2>
          <Separator />
          {data.catalog.content.length > 0 ? (
            <>
              <GameGrid games={data.catalog.content} />
              <GamesPagination
                page={page}
                totalPages={data.catalog.metadata.totalPages}
                hasNext={data.catalog.metadata.hasNext}
                hasPrevious={data.catalog.metadata.hasPrevious}
                search={search}
              />
            </>
          ) : (
            <p className="py-8 text-center text-muted-foreground">
              No games to display.
            </p>
          )}
        </>
      )}
    </div>
  )
}
