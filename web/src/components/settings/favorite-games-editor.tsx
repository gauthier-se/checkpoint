import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Gamepad2, GripVertical, X } from 'lucide-react'
import { toast } from 'sonner'
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core'
import {
  SortableContext,
  arrayMove,
  horizontalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { DragEndEvent } from '@dnd-kit/core'
import type { FavoriteGame } from '@/types/profile'
import type { Game } from '@/types/game'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ListGameSearch } from '@/components/lists/list-game-search'
import { updateFavorites } from '@/queries/profile'
import { isApiError } from '@/services/api'

const MAX_FAVORITES = 5

interface FavoriteGamesEditorProps {
  username: string
  initialFavorites: Array<FavoriteGame>
  /** When true, render the bare content without the surrounding Card chrome (used in the onboarding wizard). */
  embedded?: boolean
  /** Called after each successful save with the resulting favorites. */
  onSaved?: (favorites: Array<FavoriteGame>) => void
}

export function FavoriteGamesEditor({
  username,
  initialFavorites,
  embedded = false,
  onSaved,
}: FavoriteGamesEditorProps) {
  const queryClient = useQueryClient()
  const [favorites, setFavorites] =
    useState<Array<FavoriteGame>>(initialFavorites)

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  )

  const mutation = useMutation({
    mutationFn: (gameIds: Array<string>) => updateFavorites(gameIds),
    onSuccess: (returned) => {
      // Trust the server's response (re-derives displayOrder).
      setFavorites(returned)
      void queryClient.invalidateQueries({
        queryKey: ['users', username, 'profile'],
      })
      onSaved?.(returned)
    },
  })

  function save(next: Array<FavoriteGame>) {
    const previous = favorites
    setFavorites(next)
    mutation.mutate(
      next.map((f) => f.gameId),
      {
        onError: (err) => {
          setFavorites(previous)
          const message =
            isApiError(err) && err.message
              ? err.message
              : 'Failed to update favorites'
          toast.error(message)
        },
      },
    )
  }

  function handleAdd(game: Game) {
    if (favorites.length >= MAX_FAVORITES) return
    const next: Array<FavoriteGame> = [
      ...favorites,
      {
        gameId: game.id,
        title: game.title,
        coverUrl: game.coverUrl,
        displayOrder: favorites.length,
      },
    ]
    save(next)
  }

  function handleRemove(gameId: string) {
    save(favorites.filter((f) => f.gameId !== gameId))
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (!over || active.id === over.id) return

    const oldIndex = favorites.findIndex((f) => f.gameId === active.id)
    const newIndex = favorites.findIndex((f) => f.gameId === over.id)
    if (oldIndex < 0 || newIndex < 0) return

    save(arrayMove(favorites, oldIndex, newIndex))
  }

  const canAdd = favorites.length < MAX_FAVORITES
  const excludeIds = favorites.map((f) => f.gameId)

  const body = (
    <>
      {favorites.length > 0 ? (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext
            items={favorites.map((f) => f.gameId)}
            strategy={horizontalListSortingStrategy}
          >
            <ul className="grid grid-cols-2 gap-3 sm:grid-cols-5">
              {favorites.map((favorite) => (
                <SortableFavoriteSlot
                  key={favorite.gameId}
                  favorite={favorite}
                  onRemove={() => handleRemove(favorite.gameId)}
                />
              ))}
            </ul>
          </SortableContext>
        </DndContext>
      ) : (
        <p className="text-muted-foreground text-sm">
          No favorites yet. Search below to add one.
        </p>
      )}

      {canAdd ? (
        <ListGameSearch onSelect={handleAdd} excludeIds={excludeIds} />
      ) : (
        <p className="text-muted-foreground text-sm">
          You've reached the limit of {MAX_FAVORITES} favorites. Remove one to
          add another.
        </p>
      )}
    </>
  )

  if (embedded) {
    return <div className="space-y-4">{body}</div>
  }

  return (
    <Card id="favorites">
      <CardHeader>
        <CardTitle>Favorite games</CardTitle>
        <CardDescription>
          Pick up to {MAX_FAVORITES} favorite games to feature on your profile.
          Drag to reorder.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">{body}</CardContent>
    </Card>
  )
}

interface SortableFavoriteSlotProps {
  favorite: FavoriteGame
  onRemove: () => void
}

function SortableFavoriteSlot({
  favorite,
  onRemove,
}: SortableFavoriteSlotProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: favorite.gameId })

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <li
      ref={setNodeRef}
      style={style}
      className="bg-card relative flex flex-col gap-2 rounded-md border p-2"
    >
      <button
        type="button"
        className="text-muted-foreground hover:text-foreground absolute right-1 top-1 z-10 rounded-full bg-background/80 p-1 transition-colors"
        onClick={onRemove}
        aria-label={`Remove ${favorite.title}`}
      >
        <X className="size-4" />
      </button>
      <button
        type="button"
        className="text-muted-foreground hover:text-foreground absolute left-1 top-1 z-10 cursor-grab touch-none rounded-full bg-background/80 p-1 transition-colors active:cursor-grabbing"
        aria-label={`Reorder ${favorite.title}`}
        {...attributes}
        {...listeners}
      >
        <GripVertical className="size-4" />
      </button>
      {favorite.coverUrl ? (
        <img
          src={favorite.coverUrl}
          alt={favorite.title}
          title={favorite.title}
          className="aspect-[3/4] w-full rounded-md object-cover"
        />
      ) : (
        <div
          className="bg-muted flex aspect-[3/4] w-full items-center justify-center rounded-md"
          title={favorite.title}
        >
          <Gamepad2 className="text-muted-foreground size-8" />
        </div>
      )}
    </li>
  )
}
