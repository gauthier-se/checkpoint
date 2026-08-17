import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import type { AdminNews } from '@/types/admin'
import { Button } from '@/components/ui/button'
import { isPublished } from '@/lib/admin-news'
import {
  invalidateNews,
  publishAdminNews,
  unpublishAdminNews,
} from '@/queries/admin/news'

/**
 * Publishing is reversible and low-stakes, so it is a single click rather than
 * a confirmation dialog — unlike the destructive actions elsewhere in the panel.
 */
export function PublishNewsButton({ article }: { article: AdminNews }) {
  const queryClient = useQueryClient()
  const published = isPublished(article)

  const mutation = useMutation({
    mutationFn: () =>
      published ? unpublishAdminNews(article.id) : publishAdminNews(article.id),
    onSuccess: async () => {
      toast.success(
        published
          ? `“${article.title}” is back to a draft`
          : `“${article.title}” is live`,
      )
      await invalidateNews(queryClient)
    },
  })

  return (
    <Button
      variant="outline"
      size="sm"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
      {published ? 'Unpublish' : 'Publish'}
    </Button>
  )
}
