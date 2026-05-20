import { queryOptions } from '@tanstack/react-query'
import type {
  LikeResponse,
  Review,
  ReviewCard,
  ReviewsResponse,
} from '@/types/review'
import { apiFetch } from '@/services/api'

export const gameReviewsQueryOptions = (
  gameId: string,
  page: number = 0,
  size: number = 10,
) => {
  return queryOptions({
    queryKey: ['games', gameId, 'reviews', page, size],
    queryFn: async (): Promise<ReviewsResponse> => {
      const res = await apiFetch(
        `/api/games/${gameId}/reviews?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export const popularReviewsQueryOptions = (size: number = 7) => {
  return queryOptions({
    queryKey: ['reviews', 'popular', size],
    queryFn: async (): Promise<Array<ReviewCard>> => {
      const res = await apiFetch(`/api/reviews/popular?size=${size}`)
      return res.json()
    },
    staleTime: 5 * 60 * 1000,
  })
}

export const recentReviewsQueryOptions = (size: number = 7) => {
  return queryOptions({
    queryKey: ['reviews', 'recent', size],
    queryFn: async (): Promise<Array<ReviewCard>> => {
      const res = await apiFetch(`/api/reviews/recent?size=${size}`)
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export interface SubmitPlayLogReviewPayload {
  content: string
  haveSpoilers: boolean
}

export const submitPlayLogReview = async (
  playId: string,
  payload: SubmitPlayLogReviewPayload,
): Promise<Review> => {
  const res = await apiFetch(`/api/me/plays/${playId}/review`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
  return res.json()
}

export const updatePlayLogReview = async (
  playId: string,
  payload: SubmitPlayLogReviewPayload,
): Promise<Review> => {
  const res = await apiFetch(`/api/me/plays/${playId}/review`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
  return res.json()
}

export const deletePlayLogReview = async (playId: string): Promise<void> => {
  await apiFetch(`/api/me/plays/${playId}/review`, { method: 'DELETE' })
}

export interface ReportResponse {
  id: string
  content: string
  createdAt: string
}

export const reportReview = async (
  reviewId: string,
  payload: { content: string },
): Promise<ReportResponse> => {
  // The server returns 409 with a tailored message
  // ("You have already reported this review") via ErrorResponse, which
  // apiFetch surfaces as an ApiError. No need to special-case the status here.
  const res = await apiFetch(`/api/reviews/${reviewId}/report`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })
  return res.json()
}

export const toggleReviewLike = async (
  reviewId: string,
): Promise<LikeResponse> => {
  const res = await apiFetch(`/api/reviews/${reviewId}/like`, {
    method: 'POST',
  })
  return res.json()
}
