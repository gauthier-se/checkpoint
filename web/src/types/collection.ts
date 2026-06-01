import type { PaginationMetadata } from './game'
import type { TagSummary } from './tag'

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH'

// Wishlist
export interface WishResponse {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  priority: Priority | null
  addedAt: string
}

export interface WishlistResponse {
  content: Array<WishResponse>
  metadata: PaginationMetadata
}

// Backlog
export interface BacklogResponse {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  priority: Priority | null
  addedAt: string
}

export interface BacklogListResponse {
  content: Array<BacklogResponse>
  metadata: PaginationMetadata
}

// Play Log
export type PlayStatus =
  | 'ARE_PLAYING'
  | 'PLAYED'
  | 'COMPLETED'
  | 'RETIRED'
  | 'SHELVED'
  | 'ABANDONED'

export interface PlayLogResponse {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  platformId: string | null
  platformName: string | null
  status: PlayStatus
  isReplay: boolean
  timePlayed: number | null
  startDate: string | null
  endDate: string | null
  ownership: string | null
  createdAt: string
  updatedAt: string
  hasReview: boolean
  reviewPreview: string | null
  score: number | null
  tags: Array<TagSummary>
}

export interface PlayLogListResponse {
  content: Array<PlayLogResponse>
  metadata: PaginationMetadata
}

// Liked games
export interface LikedGameResponse {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  likedAt: string
}

export interface LikedGameListResponse {
  content: Array<LikedGameResponse>
  metadata: PaginationMetadata
}

// Unified "All games" feed
export type CollectionType = 'LIBRARY' | 'WISHLIST' | 'BACKLOG' | 'LIKED'

export interface UnifiedGameItem {
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  collectionTypes: Array<CollectionType>
  addedAt: string
  libraryStatus: string | null
  userRating: number | null
  priority: Priority | null
}

export interface UnifiedGameListResponse {
  content: Array<UnifiedGameItem>
  metadata: PaginationMetadata
}

// Shared
export type CollectionTab =
  | 'games'
  | 'playing'
  | 'played'
  | 'completed'
  | 'retired'
  | 'shelved'
  | 'abandoned'
  | 'wishlist'
  | 'backlog'
  | 'journal'
  | 'tags'
  | 'liked'
