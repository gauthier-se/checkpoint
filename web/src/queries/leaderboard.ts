import { queryOptions } from '@tanstack/react-query'
import type { LeaderboardEntry, LeaderboardSortBy } from '@/types/leaderboard'
import { apiFetch } from '@/services/api'

export const leaderboardQueryOptions = (
  sortBy: LeaderboardSortBy = 'xp',
  limit: number = 50,
  following: boolean = false,
) => {
  return queryOptions({
    queryKey: ['leaderboard', sortBy, limit, following],
    queryFn: async (): Promise<Array<LeaderboardEntry>> => {
      const params = new URLSearchParams({
        sortBy,
        limit: String(limit),
      })
      if (following) params.set('following', 'true')
      const res = await apiFetch(`/api/leaderboard?${params.toString()}`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}
