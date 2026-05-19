import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ScoreStarsProps {
  /** Score on a 1-10 half-star scale (e.g. 7 = 3.5 stars). */
  score: number
  /** Tailwind sizing for each star. Defaults to `h-5 w-5`. */
  starClassName?: string
  /** Wrapper className override. */
  className?: string
}

/**
 * Display-only 5-star rating with half-star support, driven by a 1-10 score.
 */
export function ScoreStars({
  score,
  starClassName = 'h-5 w-5',
  className,
}: ScoreStarsProps) {
  return (
    <div className={cn('flex gap-0.5', className)}>
      {[1, 2, 3, 4, 5].map((star) => {
        const leftScore = star * 2 - 1
        const rightScore = star * 2
        const isFull = score >= rightScore
        const isHalf = score === leftScore
        return (
          <div
            key={star}
            className={cn('relative inline-block', starClassName)}
          >
            <Star
              aria-hidden
              className={cn(
                'absolute inset-0 text-muted-foreground/30',
                starClassName,
              )}
            />
            {(isFull || isHalf) && (
              <Star
                aria-hidden
                className={cn(
                  'absolute inset-0 fill-yellow-400 text-yellow-500',
                  starClassName,
                )}
                style={isHalf ? { clipPath: 'inset(0 50% 0 0)' } : undefined}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}
