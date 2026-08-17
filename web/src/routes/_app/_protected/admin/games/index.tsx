import { Suspense, useEffect, useMemo, useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { Search, X } from 'lucide-react'
import type { AdminCatalogSearchParams } from '@/types/admin'
import { AdminGamesTable } from '@/components/admin/games/admin-games-table'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { paginateClientSide } from '@/lib/admin-pagination'
import { parseTrimmedString } from '@/lib/search-params'
import {
  adminCatalogPageQueryOptions,
  adminGameSearchQueryOptions,
} from '@/queries/admin/games'

const SEARCH_PAGE_SIZE = 20

export const Route = createFileRoute('/_app/_protected/admin/games/')({
  validateSearch: (
    search: Record<string, unknown>,
  ): AdminCatalogSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseTrimmedString(search.q),
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    // The two listings come from different endpoints with different shapes, so
    // the prefetch branches rather than picking options in a ternary.
    if (deps.q) {
      void context.queryClient.prefetchQuery(
        adminGameSearchQueryOptions(deps.q),
      )
    } else {
      void context.queryClient.prefetchQuery(
        adminCatalogPageQueryOptions(deps.page),
      )
    }
  },
  component: AdminCatalogPage,
})

function AdminCatalogPage() {
  const search = Route.useSearch()

  return (
    <>
      <CatalogSearch search={search} />
      <Suspense fallback={<TableSkeleton />}>
        {search.q ? <SearchResults /> : <CatalogPage />}
      </Suspense>
    </>
  )
}

function CatalogSearch({ search }: { search: AdminCatalogSearchParams }) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(search.q ?? '')

  useEffect(() => {
    setQuery(search.q ?? '')
  }, [search.q])

  const apply = (next: string | undefined) => {
    void navigate({
      to: '/admin/games',
      search: { q: next, page: 1 },
      replace: true,
    })
  }

  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      <div className="relative min-w-56 flex-1">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') apply(query.trim() || undefined)
          }}
          onBlur={() => {
            const next = query.trim() || undefined
            if (next !== search.q) apply(next)
          }}
          placeholder="Search the catalog by title"
          aria-label="Search the catalog"
          className="pl-8"
        />
      </div>
      {search.q && (
        <Button
          variant="ghost"
          onClick={() => {
            setQuery('')
            apply(undefined)
          }}
        >
          <X className="size-4" />
          Clear
        </Button>
      )}
    </div>
  )
}

function CatalogPage() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(adminCatalogPageQueryOptions(search.page))

  return (
    <AdminGamesTable
      rows={data.content}
      metadata={data.metadata}
      search={search}
    />
  )
}

function SearchResults() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(adminGameSearchQueryOptions(search.q ?? ''))

  // The search endpoint answers with a flat list, so it is paged here.
  const page = useMemo(
    () => paginateClientSide(data, search.page, SEARCH_PAGE_SIZE),
    [data, search.page],
  )

  return (
    <AdminGamesTable
      rows={page.rows}
      metadata={page.metadata}
      search={search}
    />
  )
}

function TableSkeleton() {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-4">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-12 w-full" />
      ))}
    </div>
  )
}
