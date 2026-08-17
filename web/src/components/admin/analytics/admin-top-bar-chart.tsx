import { ClientOnly } from '@tanstack/react-router'
import {
  Bar,
  BarChart,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

export interface TopBarDatum {
  id: string
  label: string
  value: number
}

interface AdminTopBarChartProps {
  title: string
  description: string
  data: Array<TopBarDatum>
  /** Noun for the measured value, used in the tooltip. */
  valueNoun: string
  emptyMessage: string
}

const ROW_HEIGHT = 40
const BAR_SIZE = 14

function ChartTooltip({
  active,
  payload,
  valueNoun,
}: {
  active?: boolean
  payload?: Array<{ payload: TopBarDatum }>
  valueNoun: string
}) {
  const datum = payload?.[0]?.payload
  if (!active || !datum) return null

  return (
    <div className="rounded-md border bg-popover px-3 py-2 text-sm shadow-md">
      <p className="font-medium">{datum.label}</p>
      <p className="text-muted-foreground">
        {datum.value} {valueNoun}
        {datum.value === 1 ? '' : 's'}
      </p>
    </div>
  )
}

/**
 * Horizontal bars for a small ranked list. One series, so every bar carries the
 * same hue — a darker-where-bigger ramp would double-encode the length and say
 * nothing new. Values are direct-labelled, which both replaces the numeric axis
 * and keeps every value readable without hovering.
 *
 * Rendered client-side only: Recharts measures the container, which the server
 * cannot do, and a mismatched first paint is worse than a brief skeleton.
 */
export function AdminTopBarChart({
  title,
  description,
  data,
  valueNoun,
  emptyMessage,
}: AdminTopBarChartProps) {
  const height = Math.max(data.length, 1) * ROW_HEIGHT

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p
            className="flex items-center justify-center text-sm text-muted-foreground"
            style={{ height }}
          >
            {emptyMessage}
          </p>
        ) : (
          <ClientOnly
            fallback={<Skeleton className="w-full" style={{ height }} />}
          >
            <ResponsiveContainer width="100%" height={height}>
              <BarChart
                data={data}
                layout="vertical"
                margin={{ top: 0, right: 40, bottom: 0, left: 0 }}
                barCategoryGap={8}
              >
                {/* The numeric axis is hidden: the direct labels carry the
                    values, and a bare scale would only add chrome. */}
                <XAxis type="number" dataKey="value" hide />
                <YAxis
                  type="category"
                  dataKey="label"
                  width={140}
                  tickLine={false}
                  axisLine={false}
                  tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
                />
                <Tooltip
                  cursor={{ fill: 'var(--muted)', opacity: 0.4 }}
                  content={<ChartTooltip valueNoun={valueNoun} />}
                />
                <Bar
                  dataKey="value"
                  barSize={BAR_SIZE}
                  radius={[0, 4, 4, 0]}
                  isAnimationActive={false}
                >
                  {data.map((datum) => (
                    <Cell key={datum.id} fill="var(--chart-1)" />
                  ))}
                  <LabelList
                    dataKey="value"
                    position="right"
                    className="fill-muted-foreground"
                    fontSize={12}
                  />
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ClientOnly>
        )}
      </CardContent>
    </Card>
  )
}
