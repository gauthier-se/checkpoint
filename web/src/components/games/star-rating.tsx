import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from '@tanstack/react-router'
import { Star } from 'lucide-react'
import { toast } from 'sonner'
import type { GameDetail } from '@/types/game'
import type { GameInteractionStatusDto } from '@/types/interaction'
import { useAuth } from '@/hooks/use-auth'
import {
  gameInteractionStatusQueryOptions,
  rateGame,
  removeRating,
} from '@/queries/games'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

interface StarRatingProps {
  game: GameDetail
  currentRating: number | null
}

export function StarRating({ game, currentRating }: StarRatingProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const router = useRouter()
  const [hoveredScore, setHoveredScore] = useState<number | null>(null)

  const snapshotInteractionStatus = async () => {
    await queryClient.cancelQueries(
      gameInteractionStatusQueryOptions(game.id),
    )
    return queryClient.getQueryData<GameInteractionStatusDto>(
      gameInteractionStatusQueryOptions(game.id).queryKey,
    )
  }

  const rollbackInteractionStatus = (
    previous: GameInteractionStatusDto | undefined,
  ) => {
    toast.error('Failed to update rating')
    if (previous) {
      queryClient.setQueryData(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        previous,
      )
    }
  }

  const invalidateAfterRatingChange = () => {
    void queryClient.invalidateQueries(
      gameInteractionStatusQueryOptions(game.id),
    )
    void queryClient.invalidateQueries({ queryKey: ['games', game.id] })
    // The game detail page reads from the route loader, not a useQuery — so
    // invalidate the router to refetch averageRating and ratingCount.
    void router.invalidate()
  }

  const rateMutation = useMutation({
    mutationFn: (score: number) => rateGame(game.id, score),
    onMutate: async (newScore) => {
      const previous = await snapshotInteractionStatus()
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        (old) => (old ? { ...old, userRating: newScore } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      rollbackInteractionStatus(context?.previous)
    },
    onSuccess: (res) => toast.success(`Rated ${res.score}/5`),
    onSettled: invalidateAfterRatingChange,
  })

  const removeRatingMutation = useMutation({
    mutationFn: () => removeRating(game.id),
    onMutate: async () => {
      const previous = await snapshotInteractionStatus()
      queryClient.setQueryData<GameInteractionStatusDto>(
        gameInteractionStatusQueryOptions(game.id).queryKey,
        (old) => (old ? { ...old, userRating: null } : old),
      )
      return { previous }
    },
    onError: (_err, _variables, context) => {
      rollbackInteractionStatus(context?.previous)
    },
    onSuccess: () => toast.success('Rating removed'),
    onSettled: invalidateAfterRatingChange,
  })

  const handleStarClick = (score: number) => {
    const ratingAtClickTime = currentRating
    if (score === ratingAtClickTime) {
      removeRatingMutation.mutate()
    } else {
      rateMutation.mutate(score)
    }
  }

  const disabled =
    !user || rateMutation.isPending || removeRatingMutation.isPending
  const displayScore = hoveredScore ?? currentRating ?? 0

  const stars = (
    <div
      className={`flex gap-0.5 ${disabled ? 'opacity-50 pointer-events-none' : ''}`}
      onMouseLeave={() => setHoveredScore(null)}
    >
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={disabled}
          className="p-1 -ml-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-sm transition-transform active:scale-90"
          onMouseEnter={() => setHoveredScore(star)}
          onClick={() => handleStarClick(star)}
        >
          <Star
            className={`h-6 w-6 transition-colors ${
              star <= displayScore
                ? 'fill-yellow-400 text-yellow-500 hover:fill-yellow-300'
                : 'text-muted-foreground/30 hover:text-muted-foreground/50'
            }`}
          />
        </button>
      ))}
    </div>
  )

  if (!user) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="inline-block cursor-not-allowed">{stars}</div>
        </TooltipTrigger>
        <TooltipContent>
          <p>Log in to rate</p>
        </TooltipContent>
      </Tooltip>
    )
  }

  return stars
}
