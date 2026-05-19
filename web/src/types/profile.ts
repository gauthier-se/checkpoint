export interface BadgeDto {
  id: string
  name: string
  picture: string | null
  description: string | null
}

export interface FavoriteGame {
  gameId: string
  title: string
  coverUrl: string | null
  displayOrder: number
}

export interface UserProfile {
  id: string
  username: string
  bio: string | null
  picture: string | null
  level: number
  xpPoint: number
  xpThreshold: number
  isPrivate: boolean
  badges: Array<BadgeDto>
  favorites: Array<FavoriteGame>
  followerCount: number
  followingCount: number
  reviewCount: number
  wishlistCount: number
  isFollowing: boolean | null
  isOwner: boolean
  createdAt: string
}
