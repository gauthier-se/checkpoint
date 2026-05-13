export interface User {
  id: string
  username: string
  email: string
  role: string
  bio: string | null
  picture: string | null
  isPrivate: boolean
  twoFactorEnabled: boolean
  steamId: string | null
  steamDisplayName: string | null
}
