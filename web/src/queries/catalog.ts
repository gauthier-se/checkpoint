import { keepPreviousData, queryOptions } from '@tanstack/react-query'
import type {
  Game,
  GameDetail,
  Genre,
  Platform,
  RecommendedGame,
} from '@/types/game'
import { apiFetch } from '@/services/api'

export function searchGamesQueryOptions(query: string) {
  return queryOptions({
    queryKey: ['games', 'search', query],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch(
        `/api/games/search?q=${encodeURIComponent(query)}`,
      )
      return res.json()
    },
    staleTime: 30 * 1000,
    enabled: query.length >= 2,
    placeholderData: keepPreviousData,
  })
}

export function gameDetailQueryOptions(gameId: string) {
  return queryOptions({
    queryKey: ['games', gameId, 'detail'],
    queryFn: async (): Promise<GameDetail> => {
      const res = await apiFetch(`/api/games/${gameId}`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export function genresQueryOptions() {
  return queryOptions({
    queryKey: ['genres'],
    queryFn: async (): Promise<Array<Genre>> => {
      const res = await apiFetch('/api/genres')
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function trendingGamesQueryOptions() {
  return queryOptions({
    queryKey: ['games', 'trending'],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch('/api/games/trending?size=7')
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function recommendedGamesQueryOptions(size: number = 7) {
  return queryOptions({
    queryKey: ['games', 'recommended', size],
    queryFn: async (): Promise<Array<RecommendedGame>> => {
      const res = await apiFetch(`/api/me/games/recommended?size=${size}`)
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function similarGamesQueryOptions(gameId: string, size: number = 7) {
  return queryOptions({
    queryKey: ['games', gameId, 'similar', size],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch(`/api/games/${gameId}/similar?size=${size}`)
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function platformsQueryOptions() {
  return queryOptions({
    queryKey: ['platforms'],
    queryFn: async (): Promise<Array<Platform>> => {
      const res = await apiFetch('/api/platforms')
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function mostBackloggedGamesQueryOptions(size: number = 7) {
  return queryOptions({
    queryKey: ['games', 'most-backlogged', size],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch(`/api/games/most-backlogged?size=${size}`)
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export function mostWishlistedGamesQueryOptions(size: number = 7) {
  return queryOptions({
    queryKey: ['games', 'most-wishlisted', size],
    queryFn: async (): Promise<Array<Game>> => {
      const res = await apiFetch(`/api/games/most-wishlisted?size=${size}`)
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}
