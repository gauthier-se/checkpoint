import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { Users } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import type { FeedTab } from '@/types/feed'
import { FeedList, FeedListSkeleton } from '@/components/feed/feed-list'
import { FeedTabs } from '@/components/feed/feed-tabs'
import { Button } from '@/components/ui/button'
import { feedQueryOptions } from '@/queries/feed'

function ActivityShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-xl border bg-transparent">
      <div className="flex flex-row items-center justify-between gap-4 border-b px-5 py-4">
        <h2 className="font-semibold">New from friends</h2>
        <Link
          to="/feed"
          search={{ page: 1 }}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          More
        </Link>
      </div>
      <div className="px-5 py-2">{children}</div>
    </div>
  )
}

/**
 * Compact friends activity feed shown next to the welcome panel on the home
 * page. Unlike the full /feed route it shows a short preview with tab filters,
 * and a gentle empty state encouraging the user to follow people rather than
 * disappearing entirely.
 */
export function FriendsActivitySection() {
  const [tab, setTab] = useState<FeedTab>('all')
  const feedQuery = useQuery(
    feedQueryOptions(0, 5, tab === 'all' ? undefined : tab),
  )
  const items = feedQuery.data?.content
  const hasItems = items && items.length > 0

  // First load on the "All" tab: show a skeleton rather than a blank gap.
  if (tab === 'all' && feedQuery.isLoading) {
    return (
      <ActivityShell>
        <FeedListSkeleton />
      </ActivityShell>
    )
  }

  // No friend activity at all: keep the panel visible (it sits beside the
  // welcome panel) but nudge the user toward following people.
  if (tab === 'all' && !hasItems) {
    return (
      <ActivityShell>
        <div className="flex flex-col items-center gap-3 px-4 py-10 text-center">
          <div className="flex size-12 items-center justify-center rounded-full bg-primary/10">
            <Users className="size-6 text-primary" />
          </div>
          <p className="text-sm text-muted-foreground">
            Follow other players to see what they are playing, rating, and
            adding to their lists.
          </p>
          <Button asChild variant="outline" size="sm">
            <Link to="/members/all" search={{ page: 1 }}>
              Find people to follow
            </Link>
          </Button>
        </div>
      </ActivityShell>
    )
  }

  return (
    <ActivityShell>
      <FeedTabs value={tab} onValueChange={setTab} className="mt-2 mb-1" />
      {feedQuery.isLoading ? (
        <FeedListSkeleton />
      ) : hasItems ? (
        <FeedList items={items} />
      ) : (
        <p className="py-6 text-center text-sm text-muted-foreground">
          No activity to show for this filter.
        </p>
      )}
    </ActivityShell>
  )
}
