import { queryOptions } from '@tanstack/react-query'
import type { FavoriteGame, UserProfile } from '@/types/profile'
import type { ReviewsResponse } from '@/types/review'
import type { Priority } from '@/types/collection'
import type { PaginationMetadata } from '@/types/game'
import type { GameStatus } from '@/types/library'
import { apiFetch } from '@/services/api'

export interface WishlistItem {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  priority: Priority | null
  addedAt: string
}

export interface WishlistResponse {
  content: Array<WishlistItem>
  metadata: PaginationMetadata
}

export interface FollowingUser {
  id: string
  pseudo: string
  picture: string | null
}

export interface FollowingResponse {
  content: Array<FollowingUser>
  metadata: PaginationMetadata
}

export const userProfileQueryOptions = (username: string) => {
  return queryOptions({
    queryKey: ['users', username, 'profile'],
    queryFn: async (): Promise<UserProfile> => {
      const res = await apiFetch(`/api/users/${username}`)
      return res.json()
    },
    staleTime: 0,
  })
}

export const userReviewsQueryOptions = (
  username: string,
  page: number = 0,
  size: number = 10,
) => {
  return queryOptions({
    queryKey: ['users', username, 'reviews', page, size],
    queryFn: async (): Promise<ReviewsResponse> => {
      const res = await apiFetch(
        `/api/users/${username}/reviews?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export const userWishlistQueryOptions = (
  username: string,
  page: number = 0,
  size: number = 20,
) => {
  return queryOptions({
    queryKey: ['users', username, 'wishlist', page, size],
    queryFn: async (): Promise<WishlistResponse> => {
      const res = await apiFetch(
        `/api/users/${username}/wishlist?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export interface CommonGameEntry {
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  viewerStatus: GameStatus
  targetStatus: GameStatus
  /** Viewer's rating on the 5-star scale (0.5–5.0), or null if unrated. */
  viewerRating: number | null
  /** Compared user's rating on the 5-star scale (0.5–5.0), or null if unrated. */
  targetRating: number | null
  /** Absolute difference between both ratings, or null if either is unrated. */
  ratingDiff: number | null
}

export interface ProfileComparison {
  affinityScore: number
  commonGamesCount: number
  viewerLibrarySize: number
  targetLibrarySize: number
  commonGames: {
    content: Array<CommonGameEntry>
    metadata: PaginationMetadata
  }
}

export const userCompareQueryOptions = (
  username: string,
  page: number = 0,
  size: number = 20,
) => {
  return queryOptions({
    queryKey: ['users', username, 'compare', page, size],
    queryFn: async (): Promise<ProfileComparison> => {
      const res = await apiFetch(
        `/api/users/${username}/compare?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export const userFollowingQueryOptions = (
  userId: string,
  page: number = 0,
  size: number = 20,
) => {
  return queryOptions({
    queryKey: ['users', userId, 'following', page, size],
    queryFn: async (): Promise<FollowingResponse> => {
      const res = await apiFetch(
        `/api/users/${userId}/following?page=${page}&size=${size}`,
      )
      return res.json()
    },
    staleTime: 60 * 1000,
  })
}

export const toggleFollowMutation = async (userId: string): Promise<void> => {
  await apiFetch(`/api/users/${userId}/follow`, {
    method: 'POST',
  })
}

export interface UpdateProfileRequest {
  pseudo: string
  bio: string | null
  isPrivate: boolean
}

export interface ProfileUpdatedResponse {
  username: string
  bio: string | null
  picture: string | null
  isPrivate: boolean
}

export async function updateProfile(
  data: UpdateProfileRequest,
): Promise<ProfileUpdatedResponse> {
  const res = await apiFetch('/api/me/profile', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function uploadPicture(file: File): Promise<{ picture: string }> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await apiFetch('/api/me/picture', {
    method: 'POST',
    body: formData,
  })
  return res.json()
}

export async function deletePicture(): Promise<void> {
  await apiFetch('/api/me/picture', {
    method: 'DELETE',
  })
}

export async function deleteAccount(): Promise<void> {
  await apiFetch('/api/me', {
    method: 'DELETE',
  })
}

export async function exportData(): Promise<Blob> {
  const res = await apiFetch('/api/me/export')
  return res.blob()
}

export async function updateFavorites(
  gameIds: Array<string>,
): Promise<Array<FavoriteGame>> {
  const res = await apiFetch('/api/me/favorites', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ gameIds }),
  })
  return res.json()
}
