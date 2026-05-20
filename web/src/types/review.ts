import type { PaginationMetadata } from './game'
import type { PlayStatus } from './interaction'

export interface ReviewUser {
  id: string
  pseudo: string
  picture: string | null
}

export interface Review {
  id: string
  content: string
  haveSpoilers: boolean
  createdAt: string
  updatedAt: string
  user: ReviewUser
  playLogId: string | null
  platformName: string | null
  playStatus: PlayStatus | null
  isReplay: boolean | null
  likesCount: number
  hasLiked: boolean
  commentsCount: number
}

export interface LikeResponse {
  liked: boolean
  likesCount: number
}

export interface ReviewsResponse {
  content: Array<Review>
  metadata: PaginationMetadata
}

export interface ReviewCard {
  id: string
  content: string
  haveSpoilers: boolean
  createdAt: string
  user: ReviewUser
  playLogId: string | null
  platformName: string | null
  playStatus: PlayStatus | null
  isReplay: boolean | null
  likesCount: number
  hasLiked: boolean
  commentsCount: number
  gameId: string
  gameTitle: string
  gameCoverUrl: string
}
