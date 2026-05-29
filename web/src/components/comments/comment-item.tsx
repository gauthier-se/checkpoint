import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ChevronDown,
  ChevronUp,
  Heart,
  MessageSquare,
  MoreHorizontal,
  Pencil,
  Reply,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import type { Comment, CommentsResponse } from '@/types/comment'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { resolvePictureUrl } from '@/lib/picture'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { MentionText } from '@/components/shared/mention-text'
import { MentionTextarea } from '@/components/shared/mention-textarea'
import { useAuth } from '@/hooks/use-auth'
import {
  commentRepliesQueryOptions,
  deleteComment,
  postReply,
  toggleCommentLike,
  updateComment,
} from '@/queries/comment'

interface CommentItemProps {
  comment: Comment
  targetType: 'review' | 'list'
  targetId: string
  isReply?: boolean
}

export function CommentItem({
  comment,
  targetType,
  targetId,
  isReply = false,
}: CommentItemProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editContent, setEditContent] = useState('')
  const [replyOpen, setReplyOpen] = useState(false)
  const [replyContent, setReplyContent] = useState('')
  const [showReplies, setShowReplies] = useState(false)

  const repliesQuery = useQuery({
    ...commentRepliesQueryOptions(comment.id, 0, 50),
    enabled: showReplies && comment.repliesCount > 0,
  })

  const invalidateComments = () => {
    void queryClient.invalidateQueries({
      queryKey:
        targetType === 'review'
          ? ['reviews', targetId, 'comments']
          : ['lists', targetId, 'comments'],
    })
  }

  const invalidateReplies = () => {
    void queryClient.invalidateQueries({
      queryKey: ['comments', comment.id, 'replies'],
    })
  }

  const likeMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: () => toggleCommentLike(comment.id),
    onMutate: async () => {
      const queryKey =
        targetType === 'review'
          ? ['reviews', targetId, 'comments']
          : ['lists', targetId, 'comments']
      await queryClient.cancelQueries({ queryKey })

      queryClient.setQueriesData<CommentsResponse>({ queryKey }, (old) => {
        if (!old) return old
        return {
          ...old,
          content: old.content.map((c) =>
            c.id === comment.id
              ? {
                  ...c,
                  hasLiked: !c.hasLiked,
                  likesCount: c.hasLiked ? c.likesCount - 1 : c.likesCount + 1,
                }
              : c,
          ),
        }
      })
    },
    onError: () => {
      toast.error('Failed to toggle like')
      invalidateComments()
    },
    onSettled: () => {
      invalidateComments()
    },
  })

  const updateMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: ({
      commentId,
      content,
    }: {
      commentId: string
      content: string
    }) => updateComment(commentId, content),
    onSuccess: () => {
      setEditingId(null)
      setEditContent('')
      invalidateComments()
      if (isReply) {
        // Also invalidate parent's replies
        void queryClient.invalidateQueries({
          queryKey: ['comments'],
        })
      }
    },
    onError: () => {
      toast.error('Failed to update comment')
    },
  })

  const deleteMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (commentId: string) => deleteComment(commentId),
    onSuccess: () => {
      invalidateComments()
      if (isReply) {
        void queryClient.invalidateQueries({
          queryKey: ['comments'],
        })
      }
    },
    onError: () => {
      toast.error('Failed to delete comment')
    },
  })

  const replyMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (content: string) => postReply(comment.id, content),
    onSuccess: () => {
      setReplyContent('')
      setReplyOpen(false)
      setShowReplies(true)
      invalidateReplies()
      invalidateComments()
    },
    onError: () => {
      toast.error('Failed to post reply')
    },
  })

  const startEdit = () => {
    setEditingId(comment.id)
    setEditContent(comment.content)
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditContent('')
  }

  const handleSubmitEdit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!editContent.trim()) return
    updateMutation.mutate({
      commentId: comment.id,
      content: editContent.trim(),
    })
  }

  const handleSubmitReply = (e: React.FormEvent) => {
    e.preventDefault()
    if (!replyContent.trim()) return
    replyMutation.mutate(replyContent.trim())
  }

  const likeButton = (
    <Button
      variant="ghost"
      size="sm"
      className="h-7 gap-1 px-2 text-xs"
      disabled={!user || likeMutation.isPending}
      onClick={() => likeMutation.mutate()}
    >
      <Heart
        className={`size-3.5 ${comment.hasLiked ? 'fill-current text-red-500' : ''}`}
      />
      {comment.likesCount > 0 && comment.likesCount}
    </Button>
  )

  return (
    <div>
      <div className="flex gap-3">
        <Avatar className="mt-1 size-8 shrink-0">
          <AvatarImage
            src={resolvePictureUrl(comment.user.picture)}
            alt={comment.user.pseudo}
          />
          <AvatarFallback className="text-xs">
            {comment.user.pseudo.substring(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium">{comment.user.pseudo}</span>
            <span className="text-xs text-muted-foreground">
              {new Date(comment.createdAt).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
              })}
            </span>
            {comment.createdAt !== comment.updatedAt && (
              <span className="text-xs text-muted-foreground">(edited)</span>
            )}
            {user && user.id === comment.user.id && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="ml-auto h-7 w-7"
                  >
                    <MoreHorizontal className="size-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={startEdit}>
                    <Pencil className="mr-2 size-4" />
                    Edit
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    className="text-destructive"
                    onClick={() => deleteMutation.mutate(comment.id)}
                  >
                    <Trash2 className="mr-2 size-4" />
                    Delete
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>

          {editingId === comment.id ? (
            <form onSubmit={handleSubmitEdit} className="mt-2 space-y-2">
              <MentionTextarea
                className="min-h-[60px] resize-y"
                value={editContent}
                onChange={setEditContent}
              />
              <div className="flex gap-2">
                <Button
                  type="submit"
                  size="sm"
                  disabled={!editContent.trim() || updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save'}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={cancelEdit}
                >
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <p className="mt-1 text-sm leading-relaxed whitespace-pre-line">
              <MentionText content={comment.content} />
            </p>
          )}

          {/* Action buttons: like + reply */}
          <div className="mt-1 flex items-center gap-1">
            {!user ? (
              <Tooltip>
                <TooltipTrigger asChild>
                  <span className="inline-block">{likeButton}</span>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Log in to like this.</p>
                </TooltipContent>
              </Tooltip>
            ) : (
              likeButton
            )}

            {!isReply && user && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 gap-1 px-2 text-xs"
                onClick={() => setReplyOpen(!replyOpen)}
              >
                <Reply className="size-3.5" />
                Reply
              </Button>
            )}
          </div>

          {/* Inline reply form */}
          {replyOpen && (
            <form onSubmit={handleSubmitReply} className="mt-2 space-y-2">
              <MentionTextarea
                placeholder="Write a reply..."
                className="min-h-[60px] resize-y"
                value={replyContent}
                onChange={setReplyContent}
              />
              <div className="flex gap-2">
                <Button
                  type="submit"
                  size="sm"
                  disabled={!replyContent.trim() || replyMutation.isPending}
                >
                  {replyMutation.isPending ? 'Posting...' : 'Post Reply'}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setReplyOpen(false)
                    setReplyContent('')
                  }}
                >
                  Cancel
                </Button>
              </div>
            </form>
          )}

          {/* Replies thread */}
          {!isReply && comment.repliesCount > 0 && (
            <div className="mt-2">
              <Button
                variant="ghost"
                size="sm"
                className="h-7 gap-1 px-2 text-xs text-muted-foreground"
                onClick={() => setShowReplies(!showReplies)}
              >
                {showReplies ? (
                  <ChevronUp className="size-3.5" />
                ) : (
                  <ChevronDown className="size-3.5" />
                )}
                <MessageSquare className="size-3.5" />
                {showReplies
                  ? 'Hide replies'
                  : `View ${comment.repliesCount} ${comment.repliesCount === 1 ? 'reply' : 'replies'}`}
              </Button>

              {showReplies && (
                <div className="mt-2 ml-4 space-y-3 border-l-2 border-muted pl-4">
                  {repliesQuery.isLoading ? (
                    <p className="text-xs text-muted-foreground">
                      Loading replies...
                    </p>
                  ) : repliesQuery.data ? (
                    repliesQuery.data.content.map((reply) => (
                      <CommentItem
                        key={reply.id}
                        comment={reply}
                        targetType={targetType}
                        targetId={targetId}
                        isReply
                      />
                    ))
                  ) : null}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
