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

// Scores are stored as 1-10 (half-star steps). Display value = score / 2.
const STAR_SLOTS = [1, 2, 3, 4, 5] as const

export function StarRating({ game, currentRating }: StarRatingProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const router = useRouter()
  const [hoveredScore, setHoveredScore] = useState<number | null>(null)

  const snapshotInteractionStatus = async () => {
    await queryClient.cancelQueries(gameInteractionStatusQueryOptions(game.id))
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
    onSuccess: (res) => toast.success(`Rated ${(res.score / 2).toFixed(1)}/5`),
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
    if (score === currentRating) {
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
      {STAR_SLOTS.map((star) => {
        const leftScore = star * 2 - 1
        const rightScore = star * 2
        const isFullFilled = displayScore >= rightScore
        const isHalfFilled = displayScore === leftScore

        return (
          <div key={star} className="relative h-8 w-8">
            <Star
              aria-hidden
              className="absolute inset-0 h-8 w-8 text-muted-foreground/30"
            />
            {(isFullFilled || isHalfFilled) && (
              <Star
                aria-hidden
                className="absolute inset-0 h-8 w-8 fill-yellow-400 text-yellow-500"
                style={
                  isHalfFilled ? { clipPath: 'inset(0 50% 0 0)' } : undefined
                }
              />
            )}

            <button
              type="button"
              disabled={disabled}
              aria-label={`Rate ${(leftScore / 2).toFixed(1)} stars`}
              className="absolute inset-y-0 left-0 w-1/2 cursor-pointer focus-visible:outline-none"
              onMouseEnter={() => setHoveredScore(leftScore)}
              onClick={() => handleStarClick(leftScore)}
            />
            <button
              type="button"
              disabled={disabled}
              aria-label={`Rate ${(rightScore / 2).toFixed(1)} stars`}
              className="absolute inset-y-0 right-0 w-1/2 cursor-pointer focus-visible:outline-none"
              onMouseEnter={() => setHoveredScore(rightScore)}
              onClick={() => handleStarClick(rightScore)}
            />
          </div>
        )
      })}
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
