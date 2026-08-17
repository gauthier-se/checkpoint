import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AdminConfirmButton } from '@/components/admin/admin-confirm-button'
import {
  deleteAdminReview,
  invalidateModeratedContent,
} from '@/queries/admin/moderation'

interface DeleteReviewButtonProps {
  review: { id: string; authorUsername: string | null }
  size?: 'sm' | 'default'
}

export function DeleteReviewButton({
  review,
  size = 'sm',
}: DeleteReviewButtonProps) {
  const queryClient = useQueryClient()

  return (
    <AdminConfirmButton
      label="Delete"
      size={size}
      title="Delete this review?"
      description={`The review by ${review.authorUsername ?? 'a deleted account'} is removed permanently and disappears from the game page. This cannot be undone.`}
      mutationFn={() => deleteAdminReview(review.id)}
      onSuccess={async () => {
        toast.success('Review deleted')
        await invalidateModeratedContent(queryClient)
      }}
    />
  )
}
