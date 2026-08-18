import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import { ADMIN_QUERY_KEY, adminFetchJson } from './shared'
import type {
  AdminGamePayload,
  ExternalGame,
  ImportJobStatus,
} from '@/types/admin'
import type { Company, Game, GameDetail, GamesResponse } from '@/types/game'
import { apiFetch } from '@/services/api'

const ADMIN_GAMES_PATH = '/api/admin/games'
const CATALOG_PAGE_SIZE = 20

export const adminGamesQueryKey = [ADMIN_QUERY_KEY, 'games'] as const

/**
 * The catalog listing reuses the public endpoints: there is no admin-specific
 * one. Browsing is paginated by the API; searching is not, so the route pages
 * the search result client-side (see `lib/admin-pagination.ts`).
 */
export function adminCatalogPageQueryOptions(page: number) {
  return queryOptions({
    queryKey: [...adminGamesQueryKey, 'catalog', 'paged', page],
    queryFn: () =>
      adminFetchJson<GamesResponse>(
        `/api/games?page=${Math.max(0, page - 1)}&size=${CATALOG_PAGE_SIZE}&sort=releaseDate,desc`,
      ),
    placeholderData: keepPreviousData,
  })
}

export function adminGameSearchQueryOptions(query: string) {
  const trimmed = query.trim()
  return queryOptions({
    queryKey: [...adminGamesQueryKey, 'catalog', 'search', trimmed],
    queryFn: () =>
      adminFetchJson<Array<Game>>(
        `/api/games/search?q=${encodeURIComponent(trimmed)}`,
      ),
    // No `enabled` guard: the route only mounts this branch when a query is
    // present, and `useSuspenseQuery` ignores `enabled` anyway.
    placeholderData: keepPreviousData,
  })
}

/** Full game record, used to prefill the edit form. */
export function adminGameDetailQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: [...adminGamesQueryKey, 'detail', gameId],
    queryFn: () => adminFetchJson<GameDetail>(`/api/games/${gameId}`),
  })
}

export function companiesQueryOptions() {
  return queryOptions({
    queryKey: ['companies'],
    queryFn: () => adminFetchJson<Array<Company>>('/api/companies'),
    staleTime: 5 * 60 * 1000,
  })
}

export async function createAdminGame(
  payload: AdminGamePayload,
): Promise<GameDetail> {
  const res = await apiFetch(ADMIN_GAMES_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return res.json() as Promise<GameDetail>
}

export async function updateAdminGame(
  gameId: string,
  payload: AdminGamePayload,
): Promise<GameDetail> {
  const res = await apiFetch(`${ADMIN_GAMES_PATH}/${gameId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return res.json() as Promise<GameDetail>
}

/**
 * Refused with `409` and a `blockingReferences` map when the game is still
 * referenced by user data: see `parseBlockingReferences`.
 */
export async function deleteAdminGame(gameId: string): Promise<void> {
  await apiFetch(`${ADMIN_GAMES_PATH}/${gameId}`, { method: 'DELETE' })
}

// --- IGDB import -----------------------------------------------------------

export function externalGameSearchQueryOptions(query: string, limit = 20) {
  const trimmed = query.trim()
  return queryOptions({
    queryKey: [...adminGamesQueryKey, 'external', trimmed, limit],
    queryFn: () =>
      adminFetchJson<Array<ExternalGame>>(
        `/api/admin/external-games/search?query=${encodeURIComponent(trimmed)}&limit=${limit}`,
      ),
    enabled: trimmed.length >= 2,
    // IGDB is rate limited and the results are stable; do not refetch on focus.
    staleTime: 5 * 60 * 1000,
    retry: false,
  })
}

export async function importExternalGame(
  externalId: number,
): Promise<GameDetail> {
  const res = await apiFetch(`${ADMIN_GAMES_PATH}/import/${externalId}`, {
    method: 'POST',
  })
  return res.json() as Promise<GameDetail>
}

export async function startTopRatedImport(params: {
  limit: number
  minRatingCount: number
}): Promise<ImportJobStatus> {
  const res = await apiFetch(
    `${ADMIN_GAMES_PATH}/import/top-rated?limit=${params.limit}&minRatingCount=${params.minRatingCount}`,
    { method: 'POST' },
  )
  return res.json() as Promise<ImportJobStatus>
}

export async function startRecentImport(params: {
  limit: number
}): Promise<ImportJobStatus> {
  const res = await apiFetch(
    `${ADMIN_GAMES_PATH}/import/recent?limit=${params.limit}`,
    { method: 'POST' },
  )
  return res.json() as Promise<ImportJobStatus>
}

/**
 * Polls a running import. The interval is dropped once the job reaches a
 * terminal state so a finished job stops generating requests, and polling only
 * runs while a job id is set: react-query also stops it on unmount.
 */
export function importJobQueryOptions(jobId: string | null) {
  return queryOptions({
    queryKey: [...adminGamesQueryKey, 'import-job', jobId],
    queryFn: () =>
      adminFetchJson<ImportJobStatus>(
        `${ADMIN_GAMES_PATH}/import/jobs/${jobId}`,
      ),
    enabled: jobId !== null,
    refetchInterval: (query) => {
      const state = query.state.data?.state
      return state === 'COMPLETED' || state === 'FAILED' ? false : 2000
    },
  })
}
