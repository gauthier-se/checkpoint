import { cn } from '@/lib/utils'

interface RatingDistributionEntry {
  score: number
  count: number
}

interface RatingDistributionChartProps {
  /** Sparse distribution from the API: only scores with at least one rating. */
  distribution: Array<RatingDistributionEntry>
  className?: string
}

// Possible raw scores on the half-star scale (display value = score / 2).
const SCORES = Array.from({ length: 10 }, (_, i) => i + 1)

/**
 * Renders a rating distribution histogram with one bar per possible score
 * (1–10, i.e. 0.5★ to 5★). Bars are pure CSS, sized relative to the busiest
 * score. Hovering or focusing a bar reveals a tooltip with the star value,
 * the count and the share of the total.
 */
export function RatingDistributionChart({
  distribution,
  className,
}: RatingDistributionChartProps) {
  const countByScore = new Map(distribution.map((d) => [d.score, d.count]))
  const total = distribution.reduce((sum, d) => sum + d.count, 0)
  const maxCount = distribution.reduce((max, d) => Math.max(max, d.count), 0)

  if (total === 0) {
    return (
      <div className={cn('space-y-2', className)}>
        <span className="text-muted-foreground text-sm">No ratings yet</span>
      </div>
    )
  }

  return (
    <div className={cn('space-y-2', className)}>
      <div className="flex h-24 items-end gap-1">
        {SCORES.map((score) => {
          const count = countByScore.get(score) ?? 0
          const heightPct = maxCount > 0 ? (count / maxCount) * 100 : 0
          const stars = score / 2
          const percentage = total > 0 ? Math.round((count / total) * 100) : 0

          return (
            <button
              key={score}
              type="button"
              tabIndex={0}
              className="group/bar relative flex h-full flex-1 items-end focus:outline-none"
              aria-label={`${stars} stars: ${count} ${count === 1 ? 'rating' : 'ratings'} (${percentage}%)`}
            >
              <div
                className="bg-muted-foreground/30 group-hover/bar:bg-muted-foreground/60 group-focus/bar:bg-muted-foreground/60 w-full rounded-sm transition-all"
                style={{ height: `${Math.max(heightPct, count > 0 ? 4 : 2)}%` }}
              />

              {/* Tooltip */}
              <div className="bg-popover text-popover-foreground pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 -translate-x-1/2 scale-95 rounded-md border px-2 py-1 text-center text-xs whitespace-nowrap opacity-0 shadow-md transition-all group-hover/bar:scale-100 group-hover/bar:opacity-100 group-focus/bar:scale-100 group-focus/bar:opacity-100">
                <div className="font-semibold">{stars} ★</div>
                <div className="text-muted-foreground">
                  {count.toLocaleString()} ({percentage}%)
                </div>
              </div>
            </button>
          )
        })}
      </div>

      <div className="text-muted-foreground flex items-center justify-between text-xs">
        <span>1 ★</span>
        <span>5 ★</span>
      </div>
    </div>
  )
}
