import { Badge } from '@/components/ui/badge'
import { isBanned } from '@/lib/admin-users'

export function AdminUserStatusBadge({ banned }: { banned: boolean | null }) {
  return isBanned({ banned }) ? (
    <Badge variant="destructive">Banned</Badge>
  ) : (
    <Badge variant="secondary">Active</Badge>
  )
}
