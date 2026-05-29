import { Link, createFileRoute } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Gamepad2, Loader2, Lock, Pencil } from 'lucide-react'
import { toast } from 'sonner'
import type { GameListDetail } from '@/types/list'
import { listDetailQueryOptions, toggleListLike } from '@/queries/lists'
import { CommentSection } from '@/components/comments/comment-section'
import { ListGameItem } from '@/components/lists/list-game-item'
import { LikeButton } from '@/components/shared/like-button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/hooks/use-auth'
import { resolvePictureUrl } from '@/lib/picture'

export const Route = createFileRoute('/_app/lists/$listId')({
  component: RouteComponent,
  loader: async ({ params: { listId }, context }) => {
    // During SSR, cookies aren't forwarded so private lists would fail.
    // Let the client-side query handle fetching with credentials.
    if (typeof window === 'undefined') return
    await context.queryClient.ensureQueryData(listDetailQueryOptions(listId))
  },
})

function RouteComponent() {
  const { listId } = Route.useParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const {
    data: list,
    isLoading,
    error,
  } = useQuery(listDetailQueryOptions(listId))

  const likeMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: () => toggleListLike(listId),
    onMutate: async () => {
      const queryKey = listDetailQueryOptions(listId).queryKey
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<GameListDetail>(queryKey)
      queryClient.setQueryData<GameListDetail>(queryKey, (old) => {
        if (!old) return old
        return {
          ...old,
          hasLiked: !old.hasLiked,
          likesCount: old.hasLiked ? old.likesCount - 1 : old.likesCount + 1,
        }
      })
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update like')
      if (context?.previous) {
        queryClient.setQueryData(
          listDetailQueryOptions(listId).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({
        queryKey: listDetailQueryOptions(listId).queryKey,
      })
    },
  })

  if (isLoading) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-10">
        <div className="flex min-h-[40vh] items-center justify-center">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      </main>
    )
  }

  if (error || !list) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-10">
        <Link
          to="/lists"
          search={{ page: 1 }}
          className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-4" />
          Back to lists
        </Link>
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Lock className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            This list is private or does not exist.
          </p>
        </div>
      </main>
    )
  }

  const initials = list.authorPseudo.slice(0, 2).toUpperCase()
  const createdDate = new Date(list.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <Link
        to="/lists"
        search={{ page: 1 }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to lists
      </Link>

      <div className="space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h1 className="text-3xl font-bold">{list.title}</h1>
              {list.isPrivate && (
                <span className="flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                  <Lock className="size-3" />
                  Private
                </span>
              )}
            </div>
            {list.description && (
              <p className="text-muted-foreground max-w-prose">
                {list.description}
              </p>
            )}
          </div>
          {list.isOwner && (
            <Button asChild variant="outline" size="sm">
              <Link to="/lists/$listId/edit" params={{ listId }}>
                <Pencil className="size-4" />
                Edit
              </Link>
            </Button>
          )}
        </div>

        <div className="flex items-center gap-4">
          <Link
            to="/profile/$username"
            params={{ username: list.authorPseudo }}
            className="flex items-center gap-2 hover:underline"
          >
            <Avatar className="size-6">
              <AvatarImage
                src={resolvePictureUrl(list.authorPicture)}
                alt={list.authorPseudo}
              />
              <AvatarFallback className="text-[10px]">
                {initials}
              </AvatarFallback>
            </Avatar>
            <span className="text-sm font-medium">{list.authorPseudo}</span>
          </Link>
          <span className="text-sm text-muted-foreground">{createdDate}</span>
        </div>

        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <span className="flex items-center gap-1">
            <Gamepad2 className="size-4" />
            {list.videoGamesCount}{' '}
            {list.videoGamesCount === 1 ? 'game' : 'games'}
          </span>
          <LikeButton
            liked={list.hasLiked}
            likesCount={list.likesCount}
            onToggle={() => likeMutation.mutate()}
            disabled={!user}
            isPending={likeMutation.isPending}
          />
        </div>
      </div>

      <Separator className="my-6" />

      {list.entries.length > 0 ? (
        <div className="space-y-2">
          {list.entries.map((entry) => (
            <ListGameItem key={entry.videoGameId} entry={entry} />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Gamepad2 className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            This list has no games yet
          </p>
        </div>
      )}

      <Separator className="my-6" />

      <CommentSection targetType="list" targetId={listId} />
    </main>
  )
}
