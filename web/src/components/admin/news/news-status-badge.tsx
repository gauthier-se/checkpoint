import type { AdminNews } from '@/types/admin'
import { Badge } from '@/components/ui/badge'
import { isPublished } from '@/lib/admin-news'

export function NewsStatusBadge({ article }: { article: AdminNews }) {
  return isPublished(article) ? (
    <Badge variant="secondary">Published</Badge>
  ) : (
    <Badge variant="outline">Draft</Badge>
  )
}
