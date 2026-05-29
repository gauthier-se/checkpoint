import { queryOptions } from '@tanstack/react-query'
import type { FriendGameActivity, FriendWantToPlay } from '@/types/game-social'
import type { ReviewCard, ReviewsResponse } from '@/types/review'
import { apiFetch } from '@/services/api'

export const friendsActivityQueryOptions = (gameId: string) =>
  queryOptions({
    queryKey: ['games', gameId, 'friends-activity'],
    queryFn: async (): Promise<FriendGameActivity> => {
      const res = await apiFetch(`/api/games/${gameId}/friends-activity`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })

export const friendsWantToPlayQueryOptions = (gameId: string) =>
  queryOptions({
    queryKey: ['games', gameId, 'friends-want-to-play'],
    queryFn: async (): Promise<FriendWantToPlay> => {
      const res = await apiFetch(`/api/games/${gameId}/friends-want-to-play`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })

export const friendReviewsQueryOptions = (
  gameId: string,
  page: number = 0,
  size: number = 10,
) =>
  queryOptions({
    queryKey: ['games', gameId, 'reviews', 'from-friends', page, size],
    queryFn: async (): Promise<ReviewsResponse> => {
      const res = await apiFetch(
        `/api/games/${gameId}/reviews/from-friends?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })

export const popularGameReviewsQueryOptions = (
  gameId: string,
  size: number = 7,
) =>
  queryOptions({
    queryKey: ['games', gameId, 'reviews', 'popular', size],
    queryFn: async (): Promise<Array<ReviewCard>> => {
      const res = await apiFetch(
        `/api/games/${gameId}/reviews/popular?size=${size}`,
      )
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
