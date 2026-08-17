import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import { ADMIN_QUERY_KEY, adminFetchJson, buildAdminPageUrl } from './shared'
import type { QueryClient } from '@tanstack/react-query'
import type {
  AdminNews,
  AdminNewsPayload,
  AdminPagedResponse,
  ImportableNewsSource,
} from '@/types/admin'
import { apiFetch } from '@/services/api'

const NEWS_PATH = '/api/admin/news'

export const adminNewsQueryKey = [ADMIN_QUERY_KEY, 'news'] as const

export function adminNewsListQueryOptions(params: { page: number }) {
  return queryOptions({
    queryKey: [...adminNewsQueryKey, 'list', params],
    queryFn: () =>
      adminFetchJson<AdminPagedResponse<AdminNews>>(
        // The service orders by creation date regardless of the sort parameter,
        // so the default is sent and no sort control is offered.
        buildAdminPageUrl(NEWS_PATH, { page: params.page }),
      ),
    placeholderData: keepPreviousData,
  })
}

export async function createAdminNews(
  payload: AdminNewsPayload,
): Promise<AdminNews> {
  const res = await apiFetch(NEWS_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return res.json() as Promise<AdminNews>
}

export async function updateAdminNews(
  newsId: string,
  payload: AdminNewsPayload,
): Promise<AdminNews> {
  const res = await apiFetch(`${NEWS_PATH}/${newsId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return res.json() as Promise<AdminNews>
}

export async function deleteAdminNews(newsId: string): Promise<void> {
  await apiFetch(`${NEWS_PATH}/${newsId}`, { method: 'DELETE' })
}

export async function publishAdminNews(newsId: string): Promise<AdminNews> {
  const res = await apiFetch(`${NEWS_PATH}/${newsId}/publish`, {
    method: 'POST',
  })
  return res.json() as Promise<AdminNews>
}

export async function unpublishAdminNews(newsId: string): Promise<AdminNews> {
  const res = await apiFetch(`${NEWS_PATH}/${newsId}/unpublish`, {
    method: 'POST',
  })
  return res.json() as Promise<AdminNews>
}

/** Returns how many articles the run pulled in. */
export async function importAdminNews(
  source: ImportableNewsSource,
): Promise<number> {
  const res = await apiFetch(`${NEWS_PATH}/import/${source}`, {
    method: 'POST',
  })
  const body = (await res.json()) as { imported?: number }
  return body.imported ?? 0
}

/**
 * Anything that changes an article also changes what `/news` shows, and the
 * panel cannot know which public queries cached it — so both roots are marked
 * stale. Nothing public is mounted here, so this costs no refetch now.
 */
export async function invalidateNews(queryClient: QueryClient): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: adminNewsQueryKey }),
    queryClient.invalidateQueries({ queryKey: ['news'] }),
  ])
}
