import { createFileRoute } from '@tanstack/react-router'
import { Newspaper } from 'lucide-react'
import type {
  NewsListSearchParams,
  NewsResponse,
  NewsSortOption,
  NewsSource,
} from '@/types/news'
import { NewsCard } from '@/components/news/news-card'
import { NewsFilters } from '@/components/news/news-filters'
import { NewsPagination } from '@/components/news/news-pagination'
import { Separator } from '@/components/ui/separator'
import { apiFetch } from '@/services/api'
import { buildNewsUrl } from '@/queries/news'

import { seo } from '@/lib/seo'

const PAGE_SIZE = 12

const VALID_SOURCES: ReadonlyArray<NewsSource> = ['MANUAL', 'STEAM', 'RSS']
const VALID_SORTS: ReadonlyArray<NewsSortOption> = [
  'publishedAt,desc',
  'publishedAt,asc',
  'title,asc',
  'title,desc',
  'relevance',
]

function parseString(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  const trimmed = value.trim()
  return trimmed === '' ? undefined : trimmed
}

function parseSource(value: unknown): NewsSource | undefined {
  const s = parseString(value)
  return s && (VALID_SOURCES as ReadonlyArray<string>).includes(s)
    ? (s as NewsSource)
    : undefined
}

function parseSort(value: unknown): NewsSortOption | undefined {
  const s = parseString(value)
  return s && (VALID_SORTS as ReadonlyArray<string>).includes(s)
    ? (s as NewsSortOption)
    : undefined
}

export const Route = createFileRoute('/_app/news/')({
  head: () => ({
    meta: seo({ title: 'News — Checkpoint' }),
  }),
  component: RouteComponent,
  validateSearch: (search: Record<string, unknown>): NewsListSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    q: parseString(search.q),
    source: parseSource(search.source),
    feedName: parseString(search.feedName),
    videoGameId: parseString(search.videoGameId),
    publishedFrom: parseString(search.publishedFrom),
    publishedTo: parseString(search.publishedTo),
    sort: parseSort(search.sort),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps }): Promise<NewsResponse> => {
    const res = await apiFetch(buildNewsUrl(deps, PAGE_SIZE))
    return res.json()
  },
})

function RouteComponent() {
  const data = Route.useLoaderData()
  const searchParams = Route.useSearch()
  const { page } = searchParams

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
