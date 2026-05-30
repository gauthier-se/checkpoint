import { Award } from 'lucide-react'
import type { BadgeDto } from '@/types/profile'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'

interface BadgeGridProps {
  badges: Array<BadgeDto>
  /**
   * When set, only the first `limit` visible badges are rendered (earned
   * first), hidden silhouettes and the "X of Y hidden discovered" footer are
   * omitted. Used by the profile header to keep the badge section compact —
   * the full grid still lives on the dedicated badges page.
   */
  limit?: number
}

/**
 * Returns true when the badge catalog has more content than would fit in a
 * preview limited to `limit` visible badges — i.e. there are more visible
 * badges than the limit, or any hidden badges exist. Used to decide whether
 * to show a "See all" link next to a limited grid.
 */
export function hasMoreBadges(badges: Array<BadgeDto>, limit: number): boolean {
  const visibleCount = badges.filter((b) => !b.hidden || b.earned).length
  const hiddenCount = badges.filter((b) => b.hidden).length
  return visibleCount > limit || hiddenCount > 0
}

export function BadgeGrid({ badges, limit }: BadgeGridProps) {
  // Visible (non-hidden) badges always render in the grid — earned ones in
  // full colour, locked ones desaturated so the user knows what to chase.
  // Hidden badges only show as silhouettes until earned (and then in full).
  const visibleAll = badges.filter((b) => !b.hidden || b.earned)
  const hiddenLocked = badges.filter((b) => b.hidden && !b.earned)

  const hiddenTotal = badges.filter((b) => b.hidden).length
  const hiddenEarned = badges.filter((b) => b.hidden && b.earned).length

  // In preview mode we surface earned badges first so the user's wins lead.
  const visible =
    limit !== undefined
      ? [...visibleAll]
          .sort((a, b) => Number(b.earned) - Number(a.earned))
          .slice(0, limit)
      : visibleAll

  const showHiddenSilhouettes = limit === undefined
  const showHiddenFooter = limit === undefined && hiddenTotal > 0

  if (
    visible.length === 0 &&
    (!showHiddenSilhouettes || hiddenLocked.length === 0)
  ) {
    return (
      <p className="text-muted-foreground text-sm">No badges to display.</p>
    )
  }

  return (
    <TooltipProvider>
      <div id="badges" className="space-y-3">
        <div className="grid grid-cols-4 gap-3 sm:grid-cols-6 md:grid-cols-8">
          {visible.map((badge) => (
            <Tooltip key={badge.id}>
              <TooltipTrigger asChild>
                <div
                  className={cn(
                    'flex h-24 flex-col items-center justify-start gap-2 rounded-lg p-3',
                    badge.earned
                      ? 'bg-muted'
                      : 'bg-muted/30 opacity-50 grayscale',
                  )}
                >
                  {badge.picture ? (
                    <img
                      src={badge.picture}
                      alt={badge.name}
                      className="size-8"
                    />
                  ) : (
                    <Award
                      className={cn(
                        'size-8',
                        badge.earned ? 'text-primary' : 'text-muted-foreground',
                      )}
                    />
                  )}
                  <div className="flex flex-1 items-center justify-center">
                    <span
                      className={cn(
                        'text-center text-xs font-medium leading-tight line-clamp-2',
                        !badge.earned && 'text-muted-foreground',
                      )}
                    >
                      {badge.name}
                    </span>
                  </div>
                </div>
              </TooltipTrigger>
              <TooltipContent>
                {badge.description ? (
                  <p>{badge.description}</p>
                ) : (
                  <p>{badge.name}</p>
                )}
                {!badge.earned && (
                  <p className="text-muted-foreground mt-1 text-xs">
                    Not earned yet
                  </p>
                )}
              </TooltipContent>
            </Tooltip>
          ))}
          {showHiddenSilhouettes &&
            hiddenLocked.map((badge) => (
              <Tooltip key={badge.id}>
                <TooltipTrigger asChild>
                  <div className="flex h-24 flex-col items-center justify-start gap-2 rounded-lg bg-muted/30 p-3 opacity-50 grayscale">
                    <Award className="size-8 text-muted-foreground" />
                    <div className="flex flex-1 items-center justify-center">
                      <span className="text-center text-xs font-medium leading-tight text-muted-foreground line-clamp-2">
                        ???
                      </span>
                    </div>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Hidden badge — unlock it to reveal.</p>
                </TooltipContent>
              </Tooltip>
            ))}
        </div>
        {showHiddenFooter && (
          <p className="text-muted-foreground text-xs">
            {hiddenEarned} of {hiddenTotal} hidden discovered
          </p>
        )}
      </div>
    </TooltipProvider>
  )
}
