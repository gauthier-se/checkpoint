import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { List } from 'lucide-react'
import type {
  GameListSortOption,
  GameListVisibility,
  GameListsSearchParams,
} from '@/types/list'
import { searchListsQueryOptions } from '@/queries/lists'
import { ListsFilters } from '@/components/lists/lists-filters'
import { ListsGrid } from '@/components/lists/lists-grid'
import { ListsPagination } from '@/components/lists/lists-pagination'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'

import { seo } from '@/lib/seo'
import { parseTrimmedString } from '@/lib/search-params'

const PAGE_SIZE = 20

const VALID_SORTS: ReadonlyArray<GameListSortOption> = [
  'recent',
  'popular',
  'most-games',
]
const VALID_VISIBILITIES: ReadonlyArray<GameListVisibility> = ['public', 'mine']

function parseSort(value: unknown): GameListSortOption | undefined {
  const s = parseTrimmedString(value)
  return s && (VALID_SORTS as ReadonlyArray<string>).includes(s)
    ? (s as GameListSortOption)
    : undefined
}

function parseVisibility(value: unknown): GameListVisibility | undefined {
  const s = parseTrimmedString(value)
  return s && (VALID_VISIBILITIES as ReadonlyArray<string>).includes(s)
    ? (s as GameListVisibility)
    : undefined
}

function parseMinGames(value: unknown): number | undefined {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return undefined
  return Math.floor(n)
}

export const Route = createFileRoute('/_app/lists/browse')({
  head: () => ({
    meta: seo({ title: 'Browse lists — Checkpoint' }),
  }),
  component: RouteComponent,
  pendingComponent: BrowseListsSkeleton,
  pendingMs: 0,
  validateSearch: (search: Record<string, unknown>): GameListsSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseTrimmedString(search.q),
    sort: parseSort(search.sort),
    visibility: parseVisibility(search.visibility),
    author: parseTrimmedString(search.author),
    minGames: parseMinGames(search.minGames),
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ deps, context }) => {
    void context.queryClient.prefetchQuery(
      searchListsQueryOptions(deps, PAGE_SIZE),
    )
  },
})

function RouteComponent() {
  return (
    <Suspense fallback={<BrowseListsSkeleton />}>
      <BrowseListsContent />
    </Suspense>
  )
}

function BrowseListsContent() {
  const searchParams = Route.useSearch()
  const { page } = searchParams
  const { data } = useSuspenseQuery(
    searchListsQueryOptions(searchParams, PAGE_SIZE),
  )

  const hasActiveFilters =
    searchParams.q != null ||
    searchParams.sort != null ||
    searchParams.visibility != null ||
    searchParams.author != null ||
    searchParams.minGames != null

  return (
    <div className="max-w-7xl mx-auto px-4">
      <div className="mt-10">
        <h1 className="text-xl font-bold">Browse lists</h1>
      </div>

      <div className="my-8">
        <div className="py-2">
          <h2 className="text-muted-foreground font-semibold">All lists</h2>
        </div>
        <Separator />

        <div className="py-4">
          <ListsFilters search={searchParams} />
        </div>

        {data.content.length > 0 ? (
          <>
            <ListsGrid lists={data.content} />
            <ListsPagination
              page={page}
              totalPages={data.metadata.totalPages}
              hasNext={data.metadata.hasNext}
              hasPrevious={data.metadata.hasPrevious}
              search={searchParams}
            />
          </>
        ) : (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <List className="text-muted-foreground size-12" />
            <p className="text-muted-foreground text-lg">
              {hasActiveFilters
                ? 'No lists match these filters'
                : 'No lists yet'}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

function BrowseListsSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4">
      <div className="mt-10">
        <Skeleton className="h-7 w-36" />
      </div>
      <div className="my-8">
        <div className="py-2">
          <Skeleton className="h-5 w-20" />
        </div>
        <Separator />
        <div className="py-4">
          <Skeleton className="h-9 w-full" />
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full rounded-md" />
          ))}
        </div>
      </div>
    </div>
  )
}
