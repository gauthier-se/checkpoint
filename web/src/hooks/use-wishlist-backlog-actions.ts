import { useQuery } from '@tanstack/react-query'
import type { Priority } from '@/types/collection'
import type { PlayStatus } from '@/types/interaction'
import { PLAY_STATUS_LABELS } from '@/lib/play-status'
import { useAuth } from '@/hooks/use-auth'
import { useGameInteractionMutation } from '@/hooks/use-game-interaction-mutation'
import {
  gameInteractionStatusQueryOptions,
  toggleBacklog,
  toggleGameLike,
  toggleWishlist,
  updateBacklogPriority,
  updateLibraryStatus,
  updateWishlistPriority,
} from '@/queries/games'

export function useWishlistBacklogActions(
  gameId: string,
  enabled: boolean = true,
) {
  const { user } = useAuth()

  const { data: status } = useQuery({
    ...gameInteractionStatusQueryOptions(gameId),
    enabled: !!user && enabled,
  })

  const wishlistMutation = useGameInteractionMutation<Priority | null>({
    gameId,
    mutationFn: (priority) =>
      toggleWishlist(gameId, status?.inWishlist ?? false, priority),
    optimisticPatch: (old) => ({ ...old, inWishlist: !old.inWishlist }),
    errorMessage: 'Failed to update wishlist',
    successMessage: (_priority, previous) =>
      previous?.inWishlist ? 'Removed from wishlist' : 'Added to wishlist',
  })

  const backlogMutation = useGameInteractionMutation<Priority | null>({
    gameId,
    mutationFn: (priority) =>
      toggleBacklog(gameId, status?.inBacklog ?? false, priority),
    optimisticPatch: (old) => ({ ...old, inBacklog: !old.inBacklog }),
    errorMessage: 'Failed to update backlog',
    successMessage: (_priority, previous) =>
      previous?.inBacklog ? 'Removed from backlog' : 'Added to backlog',
  })

  const likeMutation = useGameInteractionMutation({
    gameId,
    mutationFn: () => toggleGameLike(gameId),
    optimisticPatch: (old) => ({ ...old, liked: !old.liked }),
    errorMessage: 'Failed to update like',
    successMessage: (_variables, previous) =>
      previous?.liked
        ? 'Removed from your liked games'
        : 'Added to your liked games',
  })

  const libraryMutation = useGameInteractionMutation<PlayStatus | null>({
    gameId,
    mutationFn: (newStatus) =>
      updateLibraryStatus(
        gameId,
        newStatus
          ? {
              videoGameId: gameId,
              status: newStatus,
              notes: status?.libraryNotes ?? null,
            }
          : null,
      ),
    optimisticPatch: (old, newStatus) => ({
      ...old,
      inLibrary: newStatus !== null,
      libraryStatus: newStatus,
    }),
    errorMessage: 'Failed to update library',
    successMessage: (newStatus) =>
      newStatus === null
        ? 'Removed from library'
        : `Library status set to ${PLAY_STATUS_LABELS[newStatus]}`,
  })

  const wishlistPriorityMutation = useGameInteractionMutation<Priority | null>({
    gameId,
    mutationFn: (priority) => updateWishlistPriority(gameId, priority),
    optimisticPatch: (old, priority) => ({
      ...old,
      wishlistPriority: priority,
    }),
    errorMessage: 'Failed to update priority',
  })

  const backlogPriorityMutation = useGameInteractionMutation<Priority | null>({
    gameId,
    mutationFn: (priority) => updateBacklogPriority(gameId, priority),
    optimisticPatch: (old, priority) => ({ ...old, backlogPriority: priority }),
    errorMessage: 'Failed to update priority',
  })

  return {
    status,
    inWishlist: status?.inWishlist ?? false,
    inBacklog: status?.inBacklog ?? false,
    liked: status?.liked ?? false,
    libraryStatus: status?.libraryStatus ?? null,
    toggleWishlist: (priority: Priority | null = null) =>
      wishlistMutation.mutate(priority),
    toggleBacklog: (priority: Priority | null = null) =>
      backlogMutation.mutate(priority),
    toggleLike: () => likeMutation.mutate(),
    // Sets the library status, or clears it when the game is already in that status.
    toggleLibraryStatus: (target: PlayStatus) =>
      libraryMutation.mutate(
        (status?.libraryStatus ?? null) === target ? null : target,
      ),
    // Sets the library status to an explicit value (null removes the game).
    setLibraryStatus: (target: PlayStatus | null) =>
      libraryMutation.mutate(target),
    setWishlistPriority: (priority: Priority | null) =>
      wishlistPriorityMutation.mutate(priority),
    setBacklogPriority: (priority: Priority | null) =>
      backlogPriorityMutation.mutate(priority),
    wishlistPending: wishlistMutation.isPending,
    backlogPending: backlogMutation.isPending,
    likePending: likeMutation.isPending,
    libraryPending: libraryMutation.isPending,
    wishlistPriorityPending: wishlistPriorityMutation.isPending,
    backlogPriorityPending: backlogPriorityMutation.isPending,
  }
}
