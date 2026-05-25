import type { PaginationMetadata } from './game'

export type FeedItemType = 'PLAY' | 'RATING' | 'REVIEW' | 'LIST' | 'LIKE_GAME'

export interface FeedUser {
  id: string
  pseudo: string
  picture: string | null
}

export interface FeedGame {
  id: string
  title: string
  coverUrl: string
  releaseDate: string | null
}

export interface FeedItem {
  id: string
  type: FeedItemType
  createdAt: string
  user: FeedUser
  game: FeedGame | null
  playStatus?: string
  score?: number
  reviewContent?: string
  haveSpoilers?: boolean
  listTitle?: string
  listGameCount?: number
}

export interface FeedResponse {
  content: Array<FeedItem>
  metadata: PaginationMetadata
}
