import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { Priority } from '@/types/collection'
import type { GameInteractionStatusDto } from '@/types/interaction'
import { useAuth } from '@/hooks/use-auth'
import {
  gameInteractionStatusQueryOptions,
  toggleBacklog,
  toggleWishlist,
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

  return {
    status,
    inWishlist: status?.inWishlist ?? false,
    inBacklog: status?.inBacklog ?? false,
    toggleWishlist: (priority: Priority | null = null) =>
      wishlistMutation.mutate(priority),
    toggleBacklog: (priority: Priority | null = null) =>
      backlogMutation.mutate(priority),
    wishlistPending: wishlistMutation.isPending,
    backlogPending: backlogMutation.isPending,
  }
}
