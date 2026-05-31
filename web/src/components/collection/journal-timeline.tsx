import { Calendar } from 'lucide-react'
import type { ReactNode } from 'react'
import type { PlayLogResponse } from '@/types/collection'
import { JournalEntry } from '@/components/collection/journal-entry'

interface JournalTimelineProps {
  /** Entries sorted most-recent first (by updatedAt). */
  entries: Array<PlayLogResponse>
  /** Optional owner actions rendered on each entry (e.g. delete). */
  renderActions?: (entry: PlayLogResponse) => ReactNode
}

interface MonthGroup {
  key: string
  label: string
  items: Array<PlayLogResponse>
}

function monthKey(dateStr: string): string {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${d.getMonth()}`
}

function monthLabel(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  })
}

/**
 * Groups journal (play log) entries by month and lays them out as a vertical
 * timeline — a month heading followed by its entries along a left rail. Shared
 * by the owner journal tab (with delete actions) and the read-only profile one.
 */
export function JournalTimeline({
  entries,
  renderActions,
}: JournalTimelineProps) {
  const groups: Array<MonthGroup> = []
  for (const entry of entries) {
    const key = monthKey(entry.updatedAt)
    // Entries arrive pre-sorted desc, so equal-month entries stay contiguous —
    // the last group is always the right one to extend.
    const last = groups[groups.length - 1]
    if (groups.length > 0 && last.key === key) {
      last.items.push(entry)
    } else {
      groups.push({ key, label: monthLabel(entry.updatedAt), items: [entry] })
    }
  }

  return (
    <div className="space-y-10">
      {groups.map((group) => (
        <section key={group.key}>
          <h3 className="mb-4 flex items-center gap-2 text-sm font-semibold tracking-wide text-muted-foreground uppercase">
            <Calendar className="size-4" />
            {group.label}
          </h3>
          <div className="flex flex-col sm:border-l sm:border-border/60 sm:pl-6">
            {group.items.map((entry) => (
              <JournalEntry
                key={entry.id}
                entry={entry}
                actions={renderActions?.(entry)}
              />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
