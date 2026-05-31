import { useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Gamepad2, Lock, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import type { GameListDetail } from '@/types/list'
import {
  deleteList,
  listDetailQueryOptions,
  toggleListLike,
} from '@/queries/lists'
import { CommentSection } from '@/components/comments/comment-section'
import { GameCard } from '@/components/games/game-card'
import { LikeButton } from '@/components/shared/like-button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuth } from '@/hooks/use-auth'
import { resolvePictureUrl } from '@/lib/picture'
import { isApiError } from '@/services/api'
import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/lists/$listId')({
  component: RouteComponent,
  loader: async ({ params: { listId }, context }) => {
    // During SSR, cookies aren't forwarded so private lists would fail.
    // Let the client-side query handle fetching with credentials.
    if (typeof window === 'undefined') return
    return context.queryClient.ensureQueryData(listDetailQueryOptions(listId))
  },
  // loaderData is undefined during SSR (see loader above), so the title falls
  // back to a generic label server-side and refines to the list name on the
  // client once the authenticated query resolves.
  head: ({ loaderData }) => ({
    meta: seo({
      title: loaderData
        ? `${loaderData.title} — Checkpoint`
        : 'List — Checkpoint',
    }),
  }),
})

function RouteComponent() {
  const { listId } = Route.useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [isDeleting, setIsDeleting] = useState(false)
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

  async function handleDelete() {
    setIsDeleting(true)
    try {
      await deleteList(listId)
      await queryClient.invalidateQueries({ queryKey: ['lists'] })
      toast.success('List deleted successfully!')
      await navigate({ to: '/lists', search: { page: 1 } })
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Failed to delete list.')
      setIsDeleting(false)
    }
  }

  if (isLoading) {
    return <ListDetailSkeleton />
  }

  if (error || !list) {
    return (
      <main className="mx-auto max-w-7xl px-4 py-10">
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
    <main className="mx-auto max-w-7xl px-4 py-10">
      <Link
        to="/lists"
        search={{ page: 1 }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to lists
      </Link>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-4">
        <aside className="space-y-5 lg:col-span-1 lg:sticky lg:top-20 lg:self-start">
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
              <p className="text-muted-foreground">{list.description}</p>
            )}
          </div>

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

          <div className="flex flex-col gap-3 text-sm text-muted-foreground">
            <span className="flex items-center gap-1">
              <Gamepad2 className="size-4" />
              {list.videoGamesCount}{' '}
              {list.videoGamesCount === 1 ? 'game' : 'games'}
            </span>
            <div className="w-fit -ml-3">
              <LikeButton
                liked={list.hasLiked}
                likesCount={list.likesCount}
                onToggle={() => likeMutation.mutate()}
                disabled={!user}
                isPending={likeMutation.isPending}
              />
            </div>
            <span>{createdDate}</span>
          </div>

          {list.isOwner && (
            <div className="flex items-center gap-2 pt-1">
              <Button asChild variant="outline" size="sm" className="flex-1">
                <Link to="/lists/$listId/edit" params={{ listId }}>
                  <Pencil className="size-4" />
                  Edit
                </Link>
              </Button>
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isDeleting}
                    className="flex-1 text-destructive hover:text-destructive"
                  >
                    <Trash2 className="size-4" />
                    Delete
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Delete this list?</AlertDialogTitle>
                    <AlertDialogDescription>
                      This action cannot be undone. The list &quot;
                      {list.title}
                      &quot; and all its entries will be permanently deleted.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={handleDelete}
                      className="bg-destructive text-white hover:bg-destructive/90"
                    >
                      Delete
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          )}
        </aside>

        <div className="lg:col-span-3">
          {list.entries.length > 0 ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
              {list.entries.map((entry) => (
                <GameCard
                  key={entry.videoGameId}
                  game={{
                    id: entry.videoGameId,
                    title: entry.title,
                    coverUrl: entry.coverUrl,
                    releaseDate: entry.releaseDate,
                  }}
                />
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
        </div>
      </div>

      <Separator className="my-8" />

      <CommentSection targetType="list" targetId={listId} />
    </main>
  )
}

function ListDetailSkeleton() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <Skeleton className="mb-6 h-4 w-24" />
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-4">
        <div className="space-y-4 lg:col-span-1">
          <Skeleton className="h-9 w-3/4" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
          <div className="flex items-center gap-2 pt-2">
            <Skeleton className="size-6 rounded-full" />
            <Skeleton className="h-4 w-24" />
          </div>
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-9 w-full" />
        </div>
        <div className="lg:col-span-3">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {Array.from({ length: 10 }).map((_, i) => (
              <Skeleton key={i} className="aspect-[3/4] w-full rounded-sm" />
            ))}
          </div>
        </div>
      </div>
    </main>
  )
}
