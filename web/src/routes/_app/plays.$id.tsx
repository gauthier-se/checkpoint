import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  Calendar,
  Clock,
  Gamepad2,
  Heart,
  Loader2,
  Lock,
  MessageSquare,
  Pencil,
  RefreshCw,
  Tag as TagIcon,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import type { PlayStatus } from '@/types/interaction'
import type { ReviewSummary } from '@/types/play-log'
import { deletePlayLog, playLogDetailQueryOptions } from '@/queries/plays'
import { gameDetailQueryOptions } from '@/queries/catalog'
import { CommentSection } from '@/components/comments/comment-section'
import { PlayLogDialog } from '@/components/games/play-log-dialog'
import { ScoreStars } from '@/components/games/score-stars'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { isApiError } from '@/services/api'

export const Route = createFileRoute('/_app/plays/$id')({
  component: PlayLogDetailPage,
  loader: async ({ params: { id }, context }) => {
    if (typeof window === 'undefined') return
    await context.queryClient.ensureQueryData(playLogDetailQueryOptions(id))
  },
})

const PLAY_STATUS_LABELS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'Playing',
  PLAYED: 'Played',
  COMPLETED: 'Completed',
  RETIRED: 'Retired',
  SHELVED: 'Shelved',
  ABANDONED: 'Abandoned',
}

const PLAY_STATUS_COLORS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  PLAYED: 'bg-violet-500/15 text-violet-400 border-violet-500/20',
  COMPLETED: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  RETIRED: 'bg-slate-500/15 text-slate-400 border-slate-500/20',
  SHELVED: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  ABANDONED: 'bg-red-500/15 text-red-400 border-red-500/20',
}

function formatTimePlayed(minutes: number): string {
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function PlayLogDetailPage() {
  const { id } = Route.useParams()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const {
    data: play,
    isLoading,
    error,
  } = useQuery(playLogDetailQueryOptions(id))

  // Editing requires platforms list; we load the game detail lazily once the
  // owner opens the dialog. Keep it stable across renders.
  const [isEditing, setIsEditing] = useState(false)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const [showSpoilers, setShowSpoilers] = useState(false)

  const { data: gameDetail } = useQuery({
    ...gameDetailQueryOptions(play?.videoGameId ?? ''),
    enabled: !!play?.videoGameId && play.isOwner && isEditing,
  })

  const deleteMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: () => deletePlayLog(id),
    onSuccess: () => {
      toast.success('Play session deleted.')
      if (play) {
        void queryClient.invalidateQueries({
          queryKey: ['users', play.username, 'profile'],
        })
        void queryClient.invalidateQueries({ queryKey: ['plays', 'me'] })
        void navigate({
          to: '/profile/$username',
          params: { username: play.username },
          search: { tab: 'reviews', page: 1 },
        })
      }
    },
    onError: (err) => {
      toast.error(
        isApiError(err) ? err.message : 'Failed to delete play session.',
      )
    },
  })

  if (isLoading) {
    return (
      <main className="mx-auto max-w-5xl px-4 py-10">
        <div className="flex min-h-[40vh] items-center justify-center">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      </main>
    )
  }

  if (error || !play) {
    const isForbidden = isApiError(error) && error.status === 403
    return (
      <main className="mx-auto max-w-5xl px-4 py-10">
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Lock className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            {isForbidden
              ? 'This play log belongs to a private profile.'
              : 'This play log does not exist or has been removed.'}
          </p>
        </div>
      </main>
    )
  }

  const authorInitials = play.username.slice(0, 2).toUpperCase()
  const createdDate = new Date(play.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <main className="mx-auto max-w-5xl px-4 py-10">
      <Link
        to="/profile/$username"
        params={{ username: play.username }}
        search={{ tab: 'reviews', page: 1 }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to {play.username}'s profile
      </Link>

      <div className="grid gap-6 md:grid-cols-[220px_1fr]">
        {/* Left column: cover + game info */}
        <div className="space-y-3">
          <Link
            to="/games/$gameId"
            params={{ gameId: play.videoGameId }}
            className="block"
          >
            <div className="aspect-[3/4] w-full overflow-hidden rounded-md bg-muted">
              {play.coverUrl ? (
                <img
                  src={play.coverUrl}
                  alt={play.title}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center bg-secondary">
                  <Gamepad2 className="size-8 text-muted-foreground" />
                </div>
              )}
            </div>
          </Link>
          <div>
            <Link
              to="/games/$gameId"
              params={{ gameId: play.videoGameId }}
              className="text-lg font-semibold leading-tight hover:underline"
            >
              {play.title}
            </Link>
            {play.releaseDate && (
              <p className="text-xs text-muted-foreground">
                ({new Date(play.releaseDate).getFullYear()})
              </p>
            )}
          </div>
          <Badge variant="outline" className="gap-1">
            <Gamepad2 className="size-3" />
            {play.platformName}
          </Badge>
        </div>

        {/* Right column: session details */}
        <div className="space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <Link
                to="/profile/$username"
                params={{ username: play.username }}
                search={{ tab: 'reviews', page: 1 }}
                className="flex items-center gap-2 hover:underline"
              >
                <Avatar className="size-8">
                  <AvatarImage
                    src={play.userPicture ?? undefined}
                    alt={play.username}
                  />
                  <AvatarFallback className="text-xs">
                    {authorInitials}
                  </AvatarFallback>
                </Avatar>
                <span className="font-medium">{play.username}</span>
              </Link>
              <span className="text-sm text-muted-foreground">
                logged on {createdDate}
              </span>
            </div>

            {play.isOwner && (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsEditing(true)}
                >
                  <Pencil className="size-4" />
                  Edit
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="text-destructive hover:text-destructive"
                  onClick={() => setIsConfirmingDelete(true)}
                >
                  <Trash2 className="size-4" />
                  Delete
                </Button>
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Badge className={PLAY_STATUS_COLORS[play.status]}>
              {PLAY_STATUS_LABELS[play.status]}
            </Badge>
            {play.isReplay && (
              <Badge variant="outline" className="gap-1">
                <RefreshCw className="size-3" />
                Replay
              </Badge>
            )}
            {play.isLikedByViewer && (
              <Badge
                variant="outline"
                className="gap-1 border-red-500/20 bg-red-500/10 text-red-500"
              >
                <Heart className="size-3 fill-current" />
                Liked the game
              </Badge>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-muted-foreground">
            {play.timePlayed != null && play.timePlayed > 0 && (
              <span className="flex items-center gap-1.5">
                <Clock className="size-4" />
                {formatTimePlayed(play.timePlayed)}
              </span>
            )}
            {play.startDate && (
              <span className="flex items-center gap-1.5">
                <Calendar className="size-4" />
                {formatDate(play.startDate)}
                {play.endDate && ` — ${formatDate(play.endDate)}`}
              </span>
            )}
            {play.ownership && (
              <span className="capitalize">{play.ownership}</span>
            )}
          </div>

          {play.score != null && (
            <div className="flex items-center gap-3">
              <ScoreStars score={play.score} />
              <span className="text-sm text-muted-foreground">
                {(play.score / 2).toFixed(1)} / 5
              </span>
            </div>
          )}

          {play.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {play.tags.map((tag) => (
                <Badge
                  key={tag.id}
                  variant="outline"
                  className="gap-1 text-muted-foreground"
                >
                  <TagIcon className="size-3" />
                  {tag.name}
                </Badge>
              ))}
            </div>
          )}

          {play.review && (
            <>
              <Separator />
              <ReviewBlock
                review={play.review}
                isOwner={play.isOwner}
                showSpoilers={showSpoilers}
                onRevealSpoilers={() => setShowSpoilers(true)}
              />
            </>
          )}
        </div>
      </div>

      {play.review && (
        <>
          <Separator className="my-8" />
          <CommentSection targetType="review" targetId={play.review.id} />
        </>
      )}

      {play.isOwner && gameDetail && (
        <PlayLogDialog
          game={gameDetail}
          open={isEditing}
          onOpenChange={setIsEditing}
          initialPlayLog={play}
          onSuccess={() => {
            void queryClient.invalidateQueries({ queryKey: ['plays', id] })
          }}
        />
      )}

      <AlertDialog
        open={isConfirmingDelete}
        onOpenChange={setIsConfirmingDelete}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this play log?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete the play session
              {play.review ? ' and its review' : ''}. This action cannot be
              undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                deleteMutation.mutate()
              }}
              disabled={deleteMutation.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  )
}

interface ReviewBlockProps {
  review: ReviewSummary
  isOwner: boolean
  showSpoilers: boolean
  onRevealSpoilers: () => void
}

function ReviewBlock({
  review,
  isOwner,
  showSpoilers,
  onRevealSpoilers,
}: ReviewBlockProps) {
  const isHidden = review.haveSpoilers && !showSpoilers && !isOwner
  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">Review</h2>
      {isHidden ? (
        <div className="space-y-2 rounded-md border border-dashed p-4">
          <p className="text-sm text-muted-foreground">
            ⚠️ This review contains spoilers.
          </p>
          <Button variant="outline" size="sm" onClick={onRevealSpoilers}>
            Show spoilers
          </Button>
        </div>
      ) : (
        <p className="whitespace-pre-wrap text-sm leading-relaxed">
          {review.content}
        </p>
      )}

      <div className="flex items-center gap-3 text-sm text-muted-foreground">
        <Tooltip>
          <TooltipTrigger asChild>
            <span className="inline-flex items-center gap-1">
              <Heart
                className={`size-4 ${review.isLikedByViewer ? 'fill-current text-red-500' : ''}`}
              />
              {review.likeCount}
            </span>
          </TooltipTrigger>
          <TooltipContent>
            <p>
              {review.likeCount} {review.likeCount === 1 ? 'like' : 'likes'}
            </p>
          </TooltipContent>
        </Tooltip>
        <span className="inline-flex items-center gap-1">
          <MessageSquare className="size-4" />
          {review.commentCount}
        </span>
      </div>
    </div>
  )
}
