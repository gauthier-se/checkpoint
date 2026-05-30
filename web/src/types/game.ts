export interface Game {
  id: string
  title: string
  coverUrl: string
  releaseDate: string
  averageRating: number | null
  ratingCount: number
}

export interface RecommendedGame {
  id: string
  title: string
  coverUrl: string
  releaseDate: string
  averageRating: number | null
  reason: string
}

export interface Genre {
  id: string
  name: string
  videoGamesCount?: number
}

export interface Platform {
  id: string
  name: string
  videoGamesCount?: number
}

export interface Company {
  id: string
  name: string
}

export interface RatingDistributionEntry {
  score: number
  count: number
}

export interface GameDetail {
  id: string
  title: string
  description: string | null
  coverUrl: string
  artworkUrl: string | null
  trailerYoutubeId: string | null
  timeToBeatNormally: number | null
  timeToBeatHastily: number | null
  timeToBeatCompletely: number | null
  releaseDate: string
  averageRating: number | null
  ratingCount: number
  ratingDistribution: Array<RatingDistributionEntry>
  genres: Array<Genre>
  platforms: Array<Platform>
  companies: Array<Company>
}

export interface PaginationMetadata {
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
}

export interface GamesResponse {
  content: Array<Game>
  metadata: PaginationMetadata
}
