import type { Priority } from './collection'
import type { GameStatus } from './library'
import type { TagSummary } from './tag'

export interface GameInteractionStatusDto {
  inWishlist: boolean
  wishlistPriority: Priority | null
  inBacklog: boolean
  backlogPriority: Priority | null
  inLibrary: boolean
  libraryStatus: GameStatus | null
  libraryNotes: string | null
  playCount: number
  userRating: number | null
  hasReview: boolean
  lastPlayRating: number | null
  liked: boolean
}

export type PlayStatus =
  | 'ARE_PLAYING'
  | 'PLAYED'
  | 'COMPLETED'
  | 'RETIRED'
  | 'SHELVED'
  | 'ABANDONED'

export interface GamePlayLogRequestDto {
  videoGameId: string
  platformId: string
  status?: PlayStatus
  startDate?: string
  endDate?: string
  timePlayed?: number
  ownership?: string
  isReplay?: boolean
  score?: number
  tagIds?: Array<string>
}

export interface GamePlayLogResponseDto {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  platformId: string
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

export interface RateResponseDto {
  id: string
  score: number
  videoGameId: string
  createdAt: string
  updatedAt: string
}
