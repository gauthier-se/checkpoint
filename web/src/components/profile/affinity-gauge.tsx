import { cn } from '@/lib/utils'

interface AffinityGaugeProps {
  /** Affinity score 0–100. */
  score: number
}

const RADIUS = 52
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/**
 * Circular gauge rendering an affinity score (0–100) with a color that shifts from
 * red (very different tastes) through neutral to emerald (great match).
 */
export function AffinityGauge({ score }: AffinityGaugeProps) {
  const clamped = Math.max(0, Math.min(100, Math.round(score)))
  const offset = CIRCUMFERENCE * (1 - clamped / 100)
  const color =
    clamped >= 80
      ? 'text-emerald-500'
      : clamped <= 20
        ? 'text-red-500'
        : 'text-primary'

  return (
    <div
      className="relative flex size-32 items-center justify-center"
      role="img"
      aria-label={`${clamped}% affinity`}
    >
      <svg className="size-32 -rotate-90" viewBox="0 0 120 120">
        <circle
          cx="60"
          cy="60"
          r={RADIUS}
          className="fill-none stroke-muted"
          strokeWidth="10"
        />
        <circle
          cx="60"
          cy="60"
          r={RADIUS}
          className={cn('fill-none transition-all duration-500', color)}
          stroke="currentColor"
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="text-3xl font-bold">{clamped}%</span>
        <span className="text-muted-foreground text-xs">affinity</span>
      </div>
    </div>
  )
}
