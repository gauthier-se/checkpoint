import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AdminNews } from '@/types/admin'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import { isPublished } from '@/lib/admin-news'
import { deleteAdminNews, invalidateNews } from '@/queries/admin/news'

export function DeleteNewsButton({ article }: { article: AdminNews }) {
  const queryClient = useQueryClient()

  return (
    <AdminConfirmButton
      label="Delete"
      title={`Delete “${article.title}”?`}
      description={
        isPublished(article)
          ? 'The article is live: deleting it removes it from the public news page immediately. This cannot be undone.'
          : 'The draft is deleted permanently. This cannot be undone.'
      }
      mutationFn={() => deleteAdminNews(article.id)}
      onSuccess={async () => {
        toast.success(`“${article.title}” deleted`)
        await invalidateNews(queryClient)
      }}
    />
  )
}
