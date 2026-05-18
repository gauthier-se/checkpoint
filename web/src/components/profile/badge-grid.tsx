import { Award } from 'lucide-react'
import type { BadgeDto } from '@/types/profile'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'

interface BadgeGridProps {
  badges: Array<BadgeDto>
}

export function BadgeGrid({ badges }: BadgeGridProps) {
  if (badges.length === 0) {
    return (
      <p className="text-muted-foreground text-sm">No badges earned yet.</p>
    )
  }

  return (
    <TooltipProvider>
      <div
        id="badges"
        className="grid grid-cols-4 gap-3 sm:grid-cols-6 md:grid-cols-8"
      >
        {badges.map((badge) => (
          <Tooltip key={badge.id}>
            <TooltipTrigger asChild>
              <div className="bg-muted flex flex-col items-center gap-1 rounded-lg p-3">
                {badge.picture ? (
                  <img
                    src={badge.picture}
                    alt={badge.name}
                    className="size-8"
                  />
                ) : (
                  <Award className="text-primary size-8" />
                )}
                <span className="text-center text-xs font-medium leading-tight">
                  {badge.name}
                </span>
              </div>
            </TooltipTrigger>
            {badge.description && (
              <TooltipContent>
                <p>{badge.description}</p>
              </TooltipContent>
            )}
          </Tooltip>
        ))}
      </div>
    </TooltipProvider>
  )
}
