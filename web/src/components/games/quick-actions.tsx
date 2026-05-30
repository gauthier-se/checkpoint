import { useEffect, useState } from 'react'

import { useHotkey } from '@tanstack/react-hotkeys'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import {
  Boxes,
  Check,
  ChevronDown,
  Gift,
  Heart,
  Library,
  NotebookPen,
  Pencil,
  StickyNote,
  Trash2,
} from 'lucide-react'
import { toast } from 'sonner'
import type { Priority } from '@/types/collection'
import type { GameDetail } from '@/types/game'
import type { GameInteractionStatusDto, PlayStatus } from '@/types/interaction'
import { PLAY_STATUS_LABELS, PLAY_STATUS_ORDER } from '@/lib/play-status'
import { NotesDialog } from '@/components/collection/notes-dialog'
import { PlayLogDialog } from '@/components/games/play-log-dialog'
import { StarRating } from '@/components/games/star-rating'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { KbdHint } from '@/components/ui/kbd'
import { useAuth } from '@/hooks/use-auth'
import { useIsDesktop } from '@/hooks/use-is-desktop'
import { useWishlistBacklogActions } from '@/hooks/use-wishlist-backlog-actions'
import {
  gameInteractionStatusQueryOptions,
  updateBacklogPriority,
  updateLibraryStatus,
  updateWishlistPriority,
} from '@/queries/games'

interface GameQuickActionsProps {
  game: GameDetail
}

export function GameQuickActions({ game }: GameQuickActionsProps) {
  const { user } = useAuth()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const queryClient = useQueryClient()
  const isDesktop = useIsDesktop()
  const [playLogOpen, setPlayLogOpen] = useState(false)
  const [notesOpen, setNotesOpen] = useState(false)

  const { data: status } = useQuery({
    ...gameInteractionStatusQueryOptions(game.id),
    enabled: !!user,
  })

  const hotkeysEnabled = isDesktop && !!user

  const {
    toggleWishlist: toggleWishlistAction,
    toggleBacklog: toggleBacklogAction,
    toggleLike: toggleLikeAction,
    wishlistPending,
    backlogPending,
    likePending,
  } = useWishlistBacklogActions(game.id)

  const wishlistPriorityMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (priority: Priority | null) =>
      updateWishlistPriority(game.id, priority),
    onMutate: async (priority) => {
      await queryClient.cancelQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        (old) => (old ? { ...old, wishlistPriority: priority } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update priority')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(game.id).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
    },
  })

  const backlogPriorityMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (priority: Priority | null) =>
      updateBacklogPriority(game.id, priority),
    onMutate: async (priority) => {
      await queryClient.cancelQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        (old) => (old ? { ...old, backlogPriority: priority } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update priority')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(game.id).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
    },
  })

  const libraryMutation = useMutation({
    meta: { suppressGlobalError: true },
    mutationFn: (newStatus: PlayStatus | null) =>
      updateLibraryStatus(
        game.id,
        newStatus
          ? {
              videoGameId: game.id,
              status: newStatus,
              notes: status?.libraryNotes ?? null,
            }
          : null,
      ),
    onMutate: async (newStatus) => {
      await queryClient.cancelQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        (old) => {
          if (!old) return old
          return {
            ...old,
            inLibrary: newStatus !== null,
            libraryStatus: newStatus,
          }
        },
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update library')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(game.id).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(game.id),
      )
    },
    onSuccess: (_data, newStatus) => {
      if (newStatus === null) {
        toast.success('Removed from library')
      } else {
        toast.success(`Library status set to ${PLAY_STATUS_LABELS[newStatus]}`)
      }
    },
  })

  useHotkey(
    'W',
    () => {
      toggleWishlistAction(status?.wishlistPriority ?? null)
    },
    { enabled: hotkeysEnabled },
  )

  useHotkey(
    'B',
    () => {
      toggleBacklogAction(status?.backlogPriority ?? null)
    },
    { enabled: hotkeysEnabled },
  )

  useHotkey(
    'L',
    () => {
      toggleLikeAction()
    },
    { enabled: hotkeysEnabled },
  )

  const handleLibraryChange = (newStatus: PlayStatus) => {
    libraryMutation.mutate(newStatus)
  }

  const handleRemoveFromLibrary = () => {
    libraryMutation.mutate(null)
  }

  const renderButtons = () => {
    const disabled = !user
    const isLiked = status?.liked ?? false
    const isWishlisted = status?.inWishlist
    const wishlistPriority = status?.wishlistPriority ?? null
    const isBacklog = status?.inBacklog
    const backlogPriority = status?.backlogPriority ?? null
    const libraryStatus = status?.libraryStatus

    const PRIORITY_LABEL: Record<Priority, string> = {
      LOW: 'Low',
      MEDIUM: 'Medium',
      HIGH: 'High',
    }

    const buttons = (
      <div className="flex flex-wrap items-center gap-2">
        {/* Like Button — a game the user loves (distinct from the wishlist) */}
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant={isLiked ? 'default' : 'outline'}
              size="sm"
              className="gap-2 focus:ring-0"
              disabled={disabled || likePending}
              aria-pressed={isLiked}
              onClick={() => toggleLikeAction()}
            >
              <Heart className={`w-4 h-4 ${isLiked ? 'fill-current' : ''}`} />
              {isLiked ? 'Liked' : 'Like'}
            </Button>
          </TooltipTrigger>
          <TooltipContent className="flex items-center gap-2">
            <span>Toggle like</span>
            <KbdHint keys={['L']} />
          </TooltipContent>
        </Tooltip>

        {/* Wishlist Button */}
        {isWishlisted ? (
          <DropdownMenu>
            <Tooltip>
              <TooltipTrigger asChild>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="default"
                    size="sm"
                    className="gap-2 focus:ring-0"
                    disabled={
                      disabled ||
                      wishlistPending ||
                      wishlistPriorityMutation.isPending
                    }
                  >
                    <Gift className="w-4 h-4 fill-current" />
                    Wishlist
                    {wishlistPriority &&
                      ` · ${PRIORITY_LABEL[wishlistPriority]}`}
                    <ChevronDown className="w-3 h-3 opacity-50" />
                  </Button>
                </DropdownMenuTrigger>
              </TooltipTrigger>
              <TooltipContent className="flex items-center gap-2">
                <span>Toggle wishlist</span>
                <KbdHint keys={['W']} />
              </TooltipContent>
            </Tooltip>
            <DropdownMenuContent align="start">
              <DropdownMenuItem
                onClick={() => wishlistPriorityMutation.mutate(null)}
              >
                {wishlistPriority === null && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={wishlistPriority === null ? '' : 'ml-6'}>
                  None
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => wishlistPriorityMutation.mutate('LOW')}
              >
                {wishlistPriority === 'LOW' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={wishlistPriority === 'LOW' ? '' : 'ml-6'}>
                  Low
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => wishlistPriorityMutation.mutate('MEDIUM')}
              >
                {wishlistPriority === 'MEDIUM' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={wishlistPriority === 'MEDIUM' ? '' : 'ml-6'}>
                  Medium
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => wishlistPriorityMutation.mutate('HIGH')}
              >
                {wishlistPriority === 'HIGH' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={wishlistPriority === 'HIGH' ? '' : 'ml-6'}>
                  High
                </span>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                className="text-destructive focus:text-destructive"
                onClick={() => toggleWishlistAction(null)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Remove from wishlist
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <DropdownMenu>
            <Tooltip>
              <TooltipTrigger asChild>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-2 focus:ring-0"
                    disabled={disabled || wishlistPending}
                  >
                    <Gift className="w-4 h-4" />
                    Wishlist
                    <ChevronDown className="w-3 h-3 opacity-50" />
                  </Button>
                </DropdownMenuTrigger>
              </TooltipTrigger>
              <TooltipContent className="flex items-center gap-2">
                <span>Toggle wishlist</span>
                <KbdHint keys={['W']} />
              </TooltipContent>
            </Tooltip>
            <DropdownMenuContent align="start">
              <DropdownMenuItem onClick={() => toggleWishlistAction(null)}>
                Add to wishlist
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => toggleWishlistAction('LOW')}>
                Add as Low
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => toggleWishlistAction('MEDIUM')}>
                Add as Medium
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => toggleWishlistAction('HIGH')}>
                Add as High
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}

        {/* Backlog Button */}
        {isBacklog ? (
          <DropdownMenu>
            <Tooltip>
              <TooltipTrigger asChild>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="default"
                    size="sm"
                    className="gap-2 focus:ring-0"
                    disabled={
                      disabled ||
                      backlogPending ||
                      backlogPriorityMutation.isPending
                    }
                  >
                    <Library className="w-4 h-4 fill-current" />
                    Backlog
                    {backlogPriority && ` · ${PRIORITY_LABEL[backlogPriority]}`}
                    <ChevronDown className="w-3 h-3 opacity-50" />
                  </Button>
                </DropdownMenuTrigger>
              </TooltipTrigger>
              <TooltipContent className="flex items-center gap-2">
                <span>Toggle backlog</span>
                <KbdHint keys={['B']} />
              </TooltipContent>
            </Tooltip>
            <DropdownMenuContent align="start">
              <DropdownMenuItem
                onClick={() => backlogPriorityMutation.mutate(null)}
              >
                {backlogPriority === null && <Check className="w-4 h-4 mr-2" />}
                <span className={backlogPriority === null ? '' : 'ml-6'}>
                  None
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => backlogPriorityMutation.mutate('LOW')}
              >
                {backlogPriority === 'LOW' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={backlogPriority === 'LOW' ? '' : 'ml-6'}>
                  Low
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => backlogPriorityMutation.mutate('MEDIUM')}
              >
                {backlogPriority === 'MEDIUM' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={backlogPriority === 'MEDIUM' ? '' : 'ml-6'}>
                  Medium
                </span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => backlogPriorityMutation.mutate('HIGH')}
              >
                {backlogPriority === 'HIGH' && (
                  <Check className="w-4 h-4 mr-2" />
                )}
                <span className={backlogPriority === 'HIGH' ? '' : 'ml-6'}>
                  High
                </span>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                className="text-destructive focus:text-destructive"
                onClick={() => toggleBacklogAction(null)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Remove from backlog
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <DropdownMenu>
            <Tooltip>
              <TooltipTrigger asChild>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-2 focus:ring-0"
                    disabled={disabled || backlogPending}
                  >
                    <Library className="w-4 h-4" />
                    Backlog
                    <ChevronDown className="w-3 h-3 opacity-50" />
                  </Button>
                </DropdownMenuTrigger>
              </TooltipTrigger>
              <TooltipContent className="flex items-center gap-2">
                <span>Toggle backlog</span>
                <KbdHint keys={['B']} />
              </TooltipContent>
            </Tooltip>
            <DropdownMenuContent align="start">
              <DropdownMenuItem onClick={() => toggleBacklogAction(null)}>
                Add to backlog
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => toggleBacklogAction('LOW')}>
                Add as Low
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => toggleBacklogAction('MEDIUM')}>
                Add as Medium
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => toggleBacklogAction('HIGH')}>
                Add as High
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}

        {/* Library Dropdown */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant={libraryStatus ? 'default' : 'outline'}
              size="sm"
              className="gap-2 focus:ring-0"
              disabled={disabled || libraryMutation.isPending}
            >
              <Boxes className="w-4 h-4" />
              {libraryStatus
                ? `Lib: ${PLAY_STATUS_LABELS[libraryStatus]}`
                : 'Library'}
              <ChevronDown className="w-3 h-3 opacity-50" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {PLAY_STATUS_ORDER.map((s) => (
              <DropdownMenuItem key={s} onClick={() => handleLibraryChange(s)}>
                {libraryStatus === s && <Check className="w-4 h-4 mr-2" />}
                <span className={libraryStatus === s ? '' : 'ml-6'}>
                  {PLAY_STATUS_LABELS[s]}
                </span>
              </DropdownMenuItem>
            ))}
            {libraryStatus && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={handleRemoveFromLibrary}
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  Remove from Library
                </DropdownMenuItem>
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Notes Button — only shown when the game is in the library */}
        {libraryStatus && (
          <Button
            variant="outline"
            size="sm"
            className="gap-2"
            disabled={disabled}
            onClick={() => setNotesOpen(true)}
          >
            <StickyNote className="w-4 h-4" />
            Notes
            {status.libraryNotes && status.libraryNotes.trim() !== '' && (
              <Pencil className="w-3 h-3" aria-label="Has notes" />
            )}
          </Button>
        )}

        {/* Play Log Button */}
        <Button
          variant="outline"
          size="sm"
          className="gap-2"
          disabled={disabled}
          onClick={() => setPlayLogOpen(true)}
        >
          <NotebookPen className="w-4 h-4" />
          Log Play
        </Button>

        {/* Rating Widget */}
        <div className="flex items-center ml-4 pl-4 border-l">
          <StarRating game={game} currentRating={status?.userRating ?? null} />
        </div>
      </div>
    )

    if (!mounted) {
      return <div className="h-9" />
    }

    if (disabled) {
      return (
        <p className="text-muted-foreground text-sm">
          <Link
            to="/login"
            className="font-medium underline hover:text-foreground"
          >
            Sign in
          </Link>{' '}
          to log, rate, or review this game.
        </p>
      )
    }

    return buttons
  }

  return (
    <>
      <div className="py-2">{renderButtons()}</div>

      <PlayLogDialog
        game={game}
        open={playLogOpen}
        onOpenChange={setPlayLogOpen}
        onSuccess={() => {
          void queryClient.invalidateQueries({
            queryKey: ['games', game.id],
          })
        }}
      />

      {status?.libraryStatus && (
        <NotesDialog
          open={notesOpen}
          onOpenChange={setNotesOpen}
          videoGameId={game.id}
          gameTitle={game.title}
          status={status.libraryStatus}
          initialNotes={status.libraryNotes}
        />
      )}
    </>
  )
}
