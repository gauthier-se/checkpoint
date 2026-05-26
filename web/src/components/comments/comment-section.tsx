import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquare } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import { CommentItem } from './comment-item'
import type { CommentsResponse } from '@/types/comment'
import { MentionTextarea } from '@/components/shared/mention-textarea'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/use-auth'
import {
  listCommentsQueryOptions,
  postListComment,
  postReviewComment,
  reviewCommentsQueryOptions,
} from '@/queries/comment'

interface CommentSectionProps {
  targetType: 'review' | 'list'
  targetId: string
}

export function CommentSection({ targetType, targetId }: CommentSectionProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [newComment, setNewComment] = useState('')
  const size = 10

  const queryOptions =
    targetType === 'review'
      ? reviewCommentsQueryOptions(targetId, page, size)
      : listCommentsQueryOptions(targetId, page, size)

  const { data, isLoading } = useQuery(queryOptions)

  const invalidateComments = () => {
    void queryClient.invalidateQueries({
      queryKey:
        targetType === 'review'
          ? ['reviews', targetId, 'comments']
          : ['lists', targetId, 'comments'],
    })
  }

  const postMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (content: string) =>
      targetType === 'review'
        ? postReviewComment(targetId, content)
        : postListComment(targetId, content),
    onSuccess: () => {
      setNewComment('')
      invalidateComments()
    },
    onError: () => {
      toast.error('Failed to post comment')
    },
  })

  const handleSubmitNew = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newComment.trim()) return
    postMutation.mutate(newComment.trim())
  }

  return (
    <div className="space-y-4">
      <h3 className="flex items-center gap-2 text-lg font-semibold">
        <MessageSquare className="size-5" />
        Comments
        {data && data.metadata.totalElements > 0 && (
          <span className="text-sm font-normal text-muted-foreground">
            ({data.metadata.totalElements})
          </span>
        )}
      </h3>

      {/* New comment form */}
      {user ? (
        <form onSubmit={handleSubmitNew} className="space-y-3">
          <div className="flex gap-3">
            <Avatar className="mt-1 size-8 shrink-0">
              <AvatarImage src="/images/default-user.jpg" alt={user.username} />
              <AvatarFallback className="text-xs">
                {user.username.substring(0, 2).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 space-y-2">
              <MentionTextarea
                placeholder="Write a comment..."
                className="min-h-[80px] resize-y"
                value={newComment}
                onChange={setNewComment}
              />
              <div className="flex justify-end">
                <Button
                  type="submit"
                  size="sm"
                  disabled={!newComment.trim() || postMutation.isPending}
                >
                  {postMutation.isPending ? 'Posting...' : 'Post Comment'}
                </Button>
              </div>
            </div>
          </div>
        </form>
      ) : (
        <p className="text-sm text-muted-foreground">
          Log in to leave a comment.
        </p>
      )}

      {/* Comments list */}
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading comments...</p>
      ) : data && data.content.length > 0 ? (
        <div className="space-y-4">
          {data.content.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              targetType={targetType}
              targetId={targetId}
            />
          ))}

          {/* Pagination */}
          {data.metadata.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={!data.metadata.hasPrevious}
                onClick={() => setPage((old) => Math.max(old - 1, 0))}
              >
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {data.metadata.page + 1} of {data.metadata.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={!data.metadata.hasNext}
                onClick={() => setPage((old) => old + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">
          No comments yet. Be the first to comment!
        </p>
      )}
    </div>
  )
}
