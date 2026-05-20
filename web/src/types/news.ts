import type { PaginationMetadata } from './game'

export interface NewsAuthor {
  id: string
  pseudo: string
  picture: string | null
}

export type NewsSource = 'MANUAL' | 'STEAM' | 'RSS'

export interface NewsArticle {
  id: string
  title: string
  description: string
  picture: string | null
  publishedAt: string
  createdAt: string
  updatedAt: string
  // Null for imported news (STEAM/RSS) — attribution lives in feedName / externalUrl.
  author: NewsAuthor | null
  source: NewsSource
  externalUrl?: string
  feedName?: string
  videoGameId?: string
}

export interface NewsResponse {
  content: Array<NewsArticle>
  metadata: PaginationMetadata
}
