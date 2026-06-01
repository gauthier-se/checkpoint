import { cn } from '@/lib/utils'

interface XpProgressBarProps {
  level: number
  xpPoint: number
  xpThreshold: number
  className?: string
}

export function XpProgressBar({
  level,
  xpPoint,
  xpThreshold,
  className,
}: XpProgressBarProps) {
  const percentage = Math.min((xpPoint / xpThreshold) * 100, 100)

  return (
    <div className={cn('space-y-1', className)}>
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium">Level {level}</span>
        <span className="text-muted-foreground">
          {xpPoint.toLocaleString()} / {xpThreshold.toLocaleString()} XP
        </span>
      </div>
      <div className="bg-secondary h-1 w-full overflow-hidden rounded-full">
        <div
          className="bg-primary h-full rounded-full transition-all duration-500"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  )
}
