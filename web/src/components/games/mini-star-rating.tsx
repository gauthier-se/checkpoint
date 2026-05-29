import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface MiniStarRatingProps {
  // 0.5–5 display score (half-step precision).
  value: number
  className?: string
}

/**
 * Compact, display-only star rendering for the friend-activity panel.
 * Only renders the filled portion of the rating — no empty-star track — so a
 * 2-star rating shows two stars (no greys after), centered under the avatar.
 * Use {@link StarRating} for the interactive rating widget.
 */
export function MiniStarRating({ value, className }: MiniStarRatingProps) {
  if (value <= 0) return null

  const fullStars = Math.floor(value)
  const hasHalf = value - fullStars >= 0.5
  const stars: Array<'full' | 'half'> = [
    ...Array.from<unknown, 'full'>({ length: fullStars }, () => 'full'),
    ...(hasHalf ? (['half'] as const) : []),
  ]

  return (
    <div
      aria-label={`${value.toFixed(1)} stars`}
      className={cn('flex justify-center gap-0.5', className)}
    >
      {stars.map((kind, i) => (
        <div key={i} className="relative h-3 w-3">
          <Star
            aria-hidden
            className="absolute inset-0 h-3 w-3 fill-yellow-400 text-yellow-500"
            style={
              kind === 'half' ? { clipPath: 'inset(0 50% 0 0)' } : undefined
            }
          />
        </div>
      ))}
    </div>
  )
}
