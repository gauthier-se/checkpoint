import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { Priority } from '@/types/collection'
import type { GameInteractionStatusDto, PlayStatus } from '@/types/interaction'
import { PLAY_STATUS_LABELS } from '@/lib/play-status'
import { useAuth } from '@/hooks/use-auth'
import {
  gameInteractionStatusQueryOptions,
  toggleBacklog,
  toggleGameLike,
  toggleWishlist,
  updateLibraryStatus,
} from '@/queries/games'

export function useWishlistBacklogActions(gameId: string) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const { data: status } = useQuery({
    ...gameInteractionStatusQueryOptions(gameId),
    enabled: !!user,
  })

  const wishlistMutation = useMutation({
    mutationFn: (priority: Priority | null) =>
      toggleWishlist(gameId, status?.inWishlist ?? false, priority),
    meta: { suppressGlobalError: true },
    onMutate: async () => {
      await queryClient.cancelQueries(gameInteractionStatusQueryOptions(gameId))
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
        (old) => (old ? { ...old, inWishlist: !old.inWishlist } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update wishlist')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(gameId).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(gameId),
      )
    },
    onSuccess: (_, __, context) => {
      if (!context.previous?.inWishlist) {
        toast.success('Added to wishlist')
      } else {
        toast.success('Removed from wishlist')
      }
    },
  })

  const backlogMutation = useMutation({
    mutationFn: (priority: Priority | null) =>
      toggleBacklog(gameId, status?.inBacklog ?? false, priority),
    meta: { suppressGlobalError: true },
    onMutate: async () => {
      await queryClient.cancelQueries(gameInteractionStatusQueryOptions(gameId))
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
        (old) => (old ? { ...old, inBacklog: !old.inBacklog } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update backlog')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(gameId).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(gameId),
      )
    },
    onSuccess: (_, __, context) => {
      if (!context.previous?.inBacklog) {
        toast.success('Added to backlog')
      } else {
        toast.success('Removed from backlog')
      }
    },
  })

  const likeMutation = useMutation({
    mutationFn: () => toggleGameLike(gameId),
    meta: { suppressGlobalError: true },
    onMutate: async () => {
      await queryClient.cancelQueries(gameInteractionStatusQueryOptions(gameId))
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
        (old) => (old ? { ...old, liked: !old.liked } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update like')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(gameId).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(gameId),
      )
    },
    onSuccess: (_, __, context) => {
      if (!context.previous?.liked) {
        toast.success('Added to your liked games')
      } else {
        toast.success('Removed from your liked games')
      }
    },
  })

  const libraryMutation = useMutation({
    mutationFn: (newStatus: PlayStatus | null) =>
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
    meta: { suppressGlobalError: true },
    onMutate: async (newStatus) => {
      await queryClient.cancelQueries(gameInteractionStatusQueryOptions(gameId))
      const previous = queryClient.getQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
      )
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(gameId).queryKey,
        (old) =>
          old
            ? {
                ...old,
                inLibrary: newStatus !== null,
                libraryStatus: newStatus,
              }
            : old,
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      toast.error('Failed to update library')
      if (context?.previous) {
        queryClient.setQueryData(
          gameInteractionStatusQueryOptions(gameId).queryKey,
          context.previous,
        )
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries(
        gameInteractionStatusQueryOptions(gameId),
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
    wishlistPending: wishlistMutation.isPending,
    backlogPending: backlogMutation.isPending,
    likePending: likeMutation.isPending,
    libraryPending: libraryMutation.isPending,
  }
}
