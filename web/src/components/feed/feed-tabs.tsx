import {
  AlignLeft,
  Heart,
  ListMusic,
  PlayCircle,
  Rss,
  Star,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { FeedTab } from '@/types/feed'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'

/** Single source of truth for the feed filter tabs (value, label, icon). */
export const FEED_TAB_OPTIONS: ReadonlyArray<{
  value: FeedTab
  label: string
  icon: LucideIcon
}> = [
  { value: 'all', label: 'All', icon: Rss },
  { value: 'PLAY', label: 'Plays', icon: PlayCircle },
  { value: 'RATING', label: 'Ratings', icon: Star },
  { value: 'REVIEW', label: 'Reviews', icon: AlignLeft },
  { value: 'LIST', label: 'Lists', icon: ListMusic },
  { value: 'LIKE_GAME', label: 'Likes', icon: Heart },
]

interface FeedTabsProps {
  value: FeedTab
  onValueChange: (value: FeedTab) => void
  className?: string
}

/**
 * Controlled filter tabs row for the activity feed. Renders only the tab
 * selector (no `TabsContent`) — the feed list is rendered separately and
 * driven by the selected value via the query key. Uses the default
 * segmented (pill) style: compact and left-aligned.
 */
export function FeedTabs({ value, onValueChange, className }: FeedTabsProps) {
  return (
    <Tabs
      value={value}
      onValueChange={(v) => onValueChange(v as FeedTab)}
      className={className}
    >
      <TabsList className="max-w-full overflow-x-auto">
        {FEED_TAB_OPTIONS.map((opt) => (
          <TabsTrigger
            key={opt.value}
            value={opt.value}
            className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground data-[state=active]:shadow-sm dark:data-[state=active]:bg-primary dark:data-[state=active]:text-primary-foreground dark:data-[state=active]:border-transparent"
          >
            <opt.icon className="size-3.5" />
            {opt.label}
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  )
}
