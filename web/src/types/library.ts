import type { PaginationMetadata } from './game'

export type GameStatus = 'PLAYING' | 'COMPLETED' | 'DROPPED'

export interface UserGameResponse {
  id: string
  videoGameId: string
  title: string
  coverUrl: string | null
  releaseDate: string | null
  status: GameStatus
  addedAt: string
  updatedAt: string
  notes: string | null
  /** The library owner's own rating, on the half-star scale (0.5–5.0), or null if unrated. */
  userRating: number | null
}

export interface UserGameRequest {
  videoGameId: string
  status: GameStatus
  notes?: string | null
}

export interface LibraryResponse {
  content: Array<UserGameResponse>
  metadata: PaginationMetadata
}
