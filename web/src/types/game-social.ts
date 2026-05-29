import type { PlayStatus } from './interaction'

export type FriendCollectionType = 'WISHLIST' | 'BACKLOG'

export interface FriendActivityEntry {
  userId: string
  pseudo: string
  picture: string | null
  primaryPlayStatus: PlayStatus | null
  rating: number | null
  hasReview: boolean
  latestPlayId: string | null
}

export interface FriendGameActivity {
  totalCount: number
  countsByPlayStatus: Partial<Record<PlayStatus, number>>
  friends: Array<FriendActivityEntry>
}

export interface FriendWishlistEntry {
  userId: string
  pseudo: string
  picture: string | null
  collectionType: FriendCollectionType
}

export interface FriendWantToPlay {
  totalCount: number
  wishlistCount: number
  backlogCount: number
  friends: Array<FriendWishlistEntry>
}
