import type { ReactNode } from 'react'
import { Card, CardContent } from '@/components/ui/card'

interface AdminStatTileProps {
  label: string
  value: number
  /** Secondary line under the value, e.g. a breakdown. */
  detail?: ReactNode
  /**
   * Renders a meter under the value for a single ratio against a limit — the
   * honest form for a two-part split, where a pie would be an anti-pattern.
   */
  ratio?: { value: number; of: number; label: string }
}

const numberFormat = new Intl.NumberFormat('en-US')

export function AdminStatTile({
  label,
  value,
  detail,
  ratio,
}: AdminStatTileProps) {
  const percent =
    ratio && ratio.of > 0
      ? Math.min(100, Math.round((ratio.value / ratio.of) * 100))
      : 0

  return (
    <Card className="py-4">
      <CardContent className="px-4">
        <p className="text-xs text-muted-foreground">{label}</p>
        {/* Proportional figures, not tabular-nums: these stand alone rather
            than aligning in a column. */}
        <p className="mt-1 text-3xl font-semibold">
          {numberFormat.format(value)}
        </p>

        {ratio && (
          <div className="mt-3">
            <div
              className="h-1.5 w-full overflow-hidden rounded-full bg-muted"
              role="meter"
              aria-valuenow={percent}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={ratio.label}
            >
              <div
                className="h-full bg-chart-1"
                style={{ width: `${percent}%` }}
              />
            </div>
            <p className="mt-1.5 text-xs text-muted-foreground">
              {ratio.label}
            </p>
          </div>
        )}

        {detail && (
          <p className="mt-1.5 text-xs text-muted-foreground">{detail}</p>
        )}
      </CardContent>
    </Card>
  )
}
