import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import type {
  NewsArticle,
  NewsListSearchParams,
  NewsResponse,
} from '@/types/news'
import { apiFetch } from '@/services/api'

const PAGE_SIZE = 12

/**
 * Builds the /api/news URL from a typed criteria object, omitting empty params.
 * The {@code page} field is 1-based in the UI and converted to 0-based for the API.
 */
export function buildNewsUrl(criteria: NewsListSearchParams, size: number) {
  const params = new URLSearchParams()
  // Always send a finite 0-based page. validateSearch normally guarantees a valid
  // page number, but a bad inbound URL or unvalidated nav could feed in NaN.
  const uiPage = Number.isFinite(criteria.page) ? Math.floor(criteria.page) : 1
  params.set('page', String(Math.max(0, uiPage - 1)))
  params.set('size', String(size))

  const append = (key: string, value: string | undefined) => {
    if (value !== undefined && value !== '') {
      params.set(key, value)
    }
  }

  append('q', criteria.q)
  append('source', criteria.source)
  append('feedName', criteria.feedName)
  append('videoGameId', criteria.videoGameId)
  append('publishedFrom', criteria.publishedFrom)
  append('publishedTo', criteria.publishedTo)
  append('sort', criteria.sort)

  return `/api/news?${params.toString()}`
}

export function newsListQueryOptions(
  criteria: NewsListSearchParams,
  size: number = PAGE_SIZE,
) {
  return queryOptions({
    queryKey: ['news', 'list', criteria, size],
    queryFn: async (): Promise<NewsResponse> => {
      const res = await apiFetch(buildNewsUrl(criteria, size))
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export function newsDetailQueryOptions(newsId: string) {
  return queryOptions({
    queryKey: ['news', newsId],
    queryFn: async (): Promise<NewsArticle> => {
      const res = await apiFetch(`/api/news/${newsId}`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export function searchNewsQueryOptions(query: string, limit: number = 5) {
  return queryOptions({
    queryKey: ['news', 'quick-search', query, limit],
    queryFn: async (): Promise<Array<NewsArticle>> => {
      const res = await apiFetch(
        `/api/news/search?q=${encodeURIComponent(query)}&limit=${limit}`,
      )
      return res.json()
    },
    staleTime: 30 * 1000,
    enabled: query.length >= 2,
    placeholderData: keepPreviousData,
  })
}
