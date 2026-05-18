export type LeaderboardSortBy = 'xp' | 'level'

export interface LeaderboardEntry {
  rank: number
  id: string
  pseudo: string
  picture: string | null
  level: number
  xpPoint: number
}
