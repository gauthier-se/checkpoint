import { queryOptions } from '@tanstack/react-query'
import type {
  LeaderboardEntry,
  LeaderboardSortBy,
} from '@/types/leaderboard'
import { apiFetch } from '@/services/api'

export const leaderboardQueryOptions = (
  sortBy: LeaderboardSortBy = 'xp',
  limit: number = 50,
) => {
  return queryOptions({
    queryKey: ['leaderboard', sortBy, limit],
    queryFn: async (): Promise<Array<LeaderboardEntry>> => {
      const res = await apiFetch(
        `/api/leaderboard?sortBy=${sortBy}&limit=${limit}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}
