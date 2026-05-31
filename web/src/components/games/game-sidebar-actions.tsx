import { useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import {
  Check,
  Gamepad2,
  Gift,
  Heart,
  Library,
  NotebookPen,
  Trash2,
} from 'lucide-react'
import { useEffect, useState } from 'react'

import type { Priority } from '@/types/collection'
import type { GameDetail } from '@/types/game'
import type { PlayStatus } from '@/types/interaction'
import { PLAY_STATUS_LABELS, PLAY_STATUS_ORDER } from '@/lib/play-status'

import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/use-auth'
import { useWishlistBacklogActions } from '@/hooks/use-wishlist-backlog-actions'

import { NotesDialog } from '@/components/collection/notes-dialog'
import { PlayLogDialog } from '@/components/games/play-log-dialog'
import { StarRating } from '@/components/games/star-rating'
import { AddToListDialog } from '@/components/lists/add-to-list-dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

const PRIORITY_LABEL: Record<Priority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
}

const PRIORITY_VALUES: ReadonlyArray<Priority> = ['LOW', 'MEDIUM', 'HIGH']

interface CollectionPriorityMenuItemsProps {
  /** Collection name used in the menu copy, e.g. "wishlist" or "backlog". */
  collectionNoun: string
  isInCollection: boolean
  currentPriority: Priority | null
  /** Adds the game to the collection with the given priority. */
  onAdd: (priority: Priority | null) => void
  /** Changes the priority while keeping the game in the collection. */
  onSetPriority: (priority: Priority | null) => void
  /** Removes the game from the collection entirely. */
  onRemove: () => void
}

/**
 * Dropdown menu body shared by the Wishlist and Backlog actions: an "add"
 * variant (when the game isn't in the collection yet) and a priority-editing
 * variant with a destructive remove entry.
 */
function CollectionPriorityMenuItems({
  collectionNoun,
  isInCollection,
  currentPriority,
  onAdd,
  onSetPriority,
  onRemove,
}: CollectionPriorityMenuItemsProps) {
  if (!isInCollection) {
    return (
      <>
        <DropdownMenuItem onClick={() => onAdd(null)}>
          Add to {collectionNoun}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        {PRIORITY_VALUES.map((priority) => (
          <DropdownMenuItem key={priority} onClick={() => onAdd(priority)}>
            Add as {PRIORITY_LABEL[priority]} priority
          </DropdownMenuItem>
        ))}
      </>
    )
  }

  return (
    <>
      <DropdownMenuItem onClick={() => onSetPriority(null)}>
        {currentPriority === null && <Check className="w-4 h-4 mr-2" />}
        <span className={currentPriority === null ? '' : 'ml-6'}>None</span>
      </DropdownMenuItem>
      {PRIORITY_VALUES.map((priority) => (
        <DropdownMenuItem
          key={priority}
          onClick={() => onSetPriority(priority)}
        >
          {currentPriority === priority && <Check className="w-4 h-4 mr-2" />}
          <span className={currentPriority === priority ? '' : 'ml-6'}>
            {PRIORITY_LABEL[priority]} priority
          </span>
        </DropdownMenuItem>
      ))}
      <DropdownMenuSeparator />
      <DropdownMenuItem
        className="text-destructive focus:text-destructive"
        onClick={onRemove}
      >
        <Trash2 className="w-4 h-4 mr-2" />
        Remove from {collectionNoun}
      </DropdownMenuItem>
    </>
  )
}

interface GameSidebarActionsProps {
  game: GameDetail
}

export function GameSidebarActions({ game }: GameSidebarActionsProps) {
  const { user } = useAuth()
  const [mounted, setMounted] = useState(false)
  const queryClient = useQueryClient()

  const [playLogOpen, setPlayLogOpen] = useState(false)
  const [notesOpen, setNotesOpen] = useState(false)
  const [addToListOpen, setAddToListOpen] = useState(false)

  const {
    status,
    toggleWishlist: toggleWishlistAction,
    toggleBacklog: toggleBacklogAction,
    toggleLike: toggleLikeAction,
    setWishlistPriority,
    setBacklogPriority,
    setLibraryStatus,
    wishlistPending,
    backlogPending,
    likePending,
    libraryPending,
    wishlistPriorityPending,
    backlogPriorityPending,
  } = useWishlistBacklogActions(game.id)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) return null

  if (!user) {
    return (
      <div className="bg-card border rounded-lg p-6 flex flex-col items-center justify-center text-center">
        <p className="text-muted-foreground text-sm mb-4">
          Sign in to log, rate, or review this game.
        </p>
        <Button asChild variant="default">
          <Link to="/login">Sign In</Link>
        </Button>
      </div>
    )
  }

  const isLiked = status?.liked ?? false
  const isWishlisted = status?.inWishlist ?? false
  const wishlistPriority = status?.wishlistPriority ?? null
  const isBacklog = status?.inBacklog ?? false
  const backlogPriority = status?.backlogPriority ?? null
  const libraryStatus = status?.libraryStatus
  const isLibrary = !!libraryStatus

  const handleLibraryChange = (newStatus: PlayStatus) => {
    setLibraryStatus(newStatus)
  }
  const handleRemoveFromLibrary = () => {
    setLibraryStatus(null)
  }

  return (
    <>
      <div className="flex flex-col bg-card border border-border/20 rounded-md overflow-hidden text-sm shadow-sm">
        {/* Top 4 Buttons */}
        <div className="grid grid-cols-4 border-b border-border/40">
          {/* Library Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                disabled={libraryPending}
                className="flex flex-col items-center justify-center gap-1.5 py-3 hover:bg-white/5 transition-colors focus:outline-none focus:bg-white/5"
              >
                <Gamepad2
                  className={cn(
                    'w-6 h-6',
                    libraryStatus ? 'text-green-500' : 'text-muted-foreground',
                  )}
                />
                <span className="text-[11px] font-medium text-foreground/80 flex items-center">
                  {libraryStatus ? PLAY_STATUS_LABELS[libraryStatus] : 'Play'}
                </span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
              {PLAY_STATUS_ORDER.map((s) => (
                <DropdownMenuItem
                  key={s}
                  onClick={() => handleLibraryChange(s)}
                >
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

          {/* Like */}
          <button
            onClick={() => toggleLikeAction()}
            disabled={likePending}
            className="flex flex-col items-center justify-center gap-1.5 py-3 hover:bg-white/5 transition-colors border-l border-border/40 focus:outline-none focus:bg-white/5"
          >
            <Heart
              className={cn(
                'w-6 h-6',
                isLiked
                  ? 'fill-orange-500 text-orange-500'
                  : 'text-muted-foreground',
              )}
            />
            <span className="text-[11px] font-medium text-foreground/80">
              Like
            </span>
          </button>

          {/* Wishlist */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                disabled={wishlistPending || wishlistPriorityPending}
                className="flex flex-col items-center justify-center gap-1.5 py-3 hover:bg-white/5 transition-colors border-l border-border/40 focus:outline-none focus:bg-white/5"
              >
                <Gift
                  className={cn(
                    'w-6 h-6',
                    isWishlisted ? 'text-blue-500' : 'text-muted-foreground',
                  )}
                />
                <span className="text-[11px] font-medium text-foreground/80">
                  {isWishlisted
                    ? wishlistPriority
                      ? PRIORITY_LABEL[wishlistPriority]
                      : 'Wishlisted'
                    : 'Wishlist'}
                </span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="center">
              <CollectionPriorityMenuItems
                collectionNoun="wishlist"
                isInCollection={isWishlisted}
                currentPriority={wishlistPriority}
                onAdd={toggleWishlistAction}
                onSetPriority={setWishlistPriority}
                onRemove={() => toggleWishlistAction(null)}
              />
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Backlog */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                disabled={backlogPending || backlogPriorityPending}
                className="flex flex-col items-center justify-center gap-1.5 py-3 hover:bg-white/5 transition-colors border-l border-border/40 focus:outline-none focus:bg-white/5"
              >
                <Library
                  className={cn(
                    'w-6 h-6',
                    isBacklog ? 'text-purple-400' : 'text-muted-foreground',
                  )}
                />
                <span className="text-[11px] font-medium text-foreground/80">
                  {isBacklog
                    ? backlogPriority
                      ? PRIORITY_LABEL[backlogPriority]
                      : 'Backlog'
                    : 'Backlog'}
                </span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <CollectionPriorityMenuItems
                collectionNoun="backlog"
                isInCollection={isBacklog}
                currentPriority={backlogPriority}
                onAdd={toggleBacklogAction}
                onSetPriority={setBacklogPriority}
                onRemove={() => toggleBacklogAction(null)}
              />
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Rate Section */}
        <div className="flex flex-col items-center justify-center py-3 border-b border-border/40 hover:bg-white/5 transition-colors">
          <span className="text-xs text-muted-foreground mb-1">Rate</span>
          <div className="scale-110">
            <StarRating
              game={game}
              currentRating={status?.userRating ?? null}
            />
          </div>
        </div>

        {/* Action List */}
        <div className="flex flex-col">
          <button
            onClick={() => setPlayLogOpen(true)}
            className="w-full text-center px-4 py-2.5 text-foreground/80 hover:bg-white/5 hover:text-foreground transition-colors border-b border-border/40 focus:outline-none focus:bg-white/5"
          >
            Review or log
          </button>

          <button
            onClick={() => setAddToListOpen(true)}
            className="w-full text-center px-4 py-2.5 text-foreground/80 hover:bg-white/5 hover:text-foreground transition-colors border-b border-border/40 focus:outline-none focus:bg-white/5"
          >
            Add to lists
          </button>

          {isLibrary && (
            <button
              onClick={() => setNotesOpen(true)}
              className="w-full flex justify-center gap-2 items-center px-4 py-2.5 text-foreground/80 hover:bg-white/5 hover:text-foreground transition-colors focus:outline-none focus:bg-white/5"
            >
              <span>Notes</span>
              {status.libraryNotes && status.libraryNotes.trim() !== '' && (
                <NotebookPen className="w-3.5 h-3.5 text-muted-foreground" />
              )}
            </button>
          )}
        </div>
      </div>

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

      <AddToListDialog
        gameId={game.id}
        gameTitle={game.title}
        open={addToListOpen}
        onOpenChange={setAddToListOpen}
      />
    </>
  )
}
