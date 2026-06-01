import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { Newspaper } from 'lucide-react'
import type {
  NewsListSearchParams,
  NewsSortOption,
  NewsSource,
} from '@/types/news'
import { newsListQueryOptions } from '@/queries/news'
import { NewsCard } from '@/components/news/news-card'
import { NewsFilters } from '@/components/news/news-filters'
import { NewsPagination } from '@/components/news/news-pagination'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'

import { seo } from '@/lib/seo'
import { parseTrimmedString } from '@/lib/search-params'

const PAGE_SIZE = 12

const VALID_SOURCES: ReadonlyArray<NewsSource> = ['MANUAL', 'STEAM', 'RSS']
const VALID_SORTS: ReadonlyArray<NewsSortOption> = [
  'publishedAt,desc',
  'publishedAt,asc',
  'title,asc',
  'title,desc',
  'relevance',
]

function parseSource(value: unknown): NewsSource | undefined {
  const s = parseTrimmedString(value)
  return s && (VALID_SOURCES as ReadonlyArray<string>).includes(s)
    ? (s as NewsSource)
    : undefined
}

function parseSort(value: unknown): NewsSortOption | undefined {
  const s = parseTrimmedString(value)
  return s && (VALID_SORTS as ReadonlyArray<string>).includes(s)
    ? (s as NewsSortOption)
    : undefined
}

export const Route = createFileRoute('/_app/news/')({
  head: () => ({
    meta: seo({ title: 'News — Checkpoint' }),
  }),
  component: RouteComponent,
  pendingComponent: NewsIndexSkeleton,
  pendingMs: 0,
  validateSearch: (search: Record<string, unknown>): NewsListSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseTrimmedString(search.q),
    source: parseSource(search.source),
    feedName: parseTrimmedString(search.feedName),
    videoGameId: parseTrimmedString(search.videoGameId),
    publishedFrom: parseTrimmedString(search.publishedFrom),
    publishedTo: parseTrimmedString(search.publishedTo),
    sort: parseSort(search.sort),
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ deps, context }) => {
    void context.queryClient.prefetchQuery(
      newsListQueryOptions(deps, PAGE_SIZE),
    )
  },
})

function RouteComponent() {
  return (
    <Suspense fallback={<NewsIndexSkeleton />}>
      <NewsListContent />
    </Suspense>
  )
}

function NewsListContent() {
  const searchParams = Route.useSearch()
  const { page } = searchParams
  const { data } = useSuspenseQuery(
    newsListQueryOptions(searchParams, PAGE_SIZE),
  )

  const hasActiveFilters =
    searchParams.q != null ||
    searchParams.source != null ||
    searchParams.feedName != null ||
    searchParams.videoGameId != null ||
    searchParams.publishedFrom != null ||
    searchParams.publishedTo != null ||
    searchParams.sort != null

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10">
        <h1 className="text-xl font-bold">News</h1>
      </div>

      <div className="my-8">
        <div className="py-2">
          <h2 className="text-muted-foreground font-semibold">
            Latest articles
          </h2>
        </div>
        <Separator />

        <div className="py-4">
          <NewsFilters search={searchParams} />
        </div>

        {data.content.length > 0 ? (
          <>
            <div className="grid grid-cols-1 gap-4 py-4 sm:grid-cols-2 lg:grid-cols-3">
              {data.content.map((article) => (
                <NewsCard key={article.id} article={article} />
              ))}
            </div>
            <NewsPagination
              page={page}
              totalPages={data.metadata.totalPages}
              hasNext={data.metadata.hasNext}
              hasPrevious={data.metadata.hasPrevious}
              search={searchParams}
            />
          </>
        ) : (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <Newspaper className="text-muted-foreground size-12" />
            <p className="text-muted-foreground text-lg">
              {hasActiveFilters ? 'No news match these filters' : 'No news yet'}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

function NewsIndexSkeleton() {
  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10">
        <Skeleton className="h-7 w-16" />
      </div>
      <div className="my-8">
        <div className="py-2">
          <Skeleton className="h-5 w-32" />
        </div>
        <Separator />
        <div className="py-4">
          <Skeleton className="h-9 w-full" />
        </div>
        <div className="grid grid-cols-1 gap-4 py-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="space-y-3 rounded-lg border p-4">
              <Skeleton className="aspect-video w-full rounded-md" />
              <div className="space-y-1.5">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-3 w-2/3" />
              </div>
              <div className="flex items-center gap-2">
                <Skeleton className="size-5 rounded-full" />
                <Skeleton className="h-3 w-24" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
