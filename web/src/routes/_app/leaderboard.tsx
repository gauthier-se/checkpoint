import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { Trophy } from 'lucide-react'
import type { LeaderboardSortBy } from '@/types/leaderboard'
import { leaderboardQueryOptions } from '@/queries/leaderboard'
import { LeaderboardTable } from '@/components/leaderboard/leaderboard-table'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useAuth } from '@/hooks/use-auth'

import { seo } from '@/lib/seo'

const LEADERBOARD_LIMIT = 50

interface LeaderboardSearch {
  sortBy: LeaderboardSortBy
  following: boolean
}

export const Route = createFileRoute('/_app/leaderboard')({
  head: () => ({
    meta: seo({ title: 'Leaderboard — Checkpoint' }),
  }),
  component: LeaderboardPage,
  validateSearch: (search: Record<string, unknown>): LeaderboardSearch => ({
    sortBy: search.sortBy === 'level' ? 'level' : 'xp',
    following: search.following === true || search.following === 'true',
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ deps, context }) =>
    context.queryClient.ensureQueryData(
      leaderboardQueryOptions(deps.sortBy, LEADERBOARD_LIMIT, deps.following),
    ),
})

function LeaderboardPage() {
  const { sortBy, following } = Route.useSearch()
  const navigate = useNavigate({ from: '/leaderboard' })
  const { user } = useAuth()
  const { data: entries } = useSuspenseQuery(
    leaderboardQueryOptions(sortBy, LEADERBOARD_LIMIT, following),
  )

  const setSort = (next: LeaderboardSortBy) => {
    if (next === sortBy) return
    void navigate({ search: (prev) => ({ ...prev, sortBy: next }) })
  }

  const setFollowing = (next: boolean) => {
    if (next === following) return
    void navigate({ search: (prev) => ({ ...prev, following: next }) })
  }

  const description = following
    ? 'People you follow, ranked among themselves.'
    : `Top ${LEADERBOARD_LIMIT} players ranked by ${
        sortBy === 'xp' ? 'XP' : 'level'
      }.`

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
        {/* Left: title + filters */}
        <aside className="space-y-6 lg:sticky lg:top-20 lg:w-64 lg:shrink-0">
          <div className="flex items-start gap-3">
            <Trophy className="size-7 shrink-0 text-yellow-500" />
            <div>
              <h1 className="text-2xl font-bold">Leaderboard</h1>
              <p className="text-sm text-muted-foreground">{description}</p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="space-y-1.5">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Sort by
              </p>
              <ButtonGroup className="w-full">
                <Button
                  variant={sortBy === 'xp' ? 'default' : 'outline'}
                  size="sm"
                  className="flex-1"
                  onClick={() => setSort('xp')}
                >
                  XP
                </Button>
                <Button
                  variant={sortBy === 'level' ? 'default' : 'outline'}
                  size="sm"
                  className="flex-1"
                  onClick={() => setSort('level')}
                >
                  Level
                </Button>
              </ButtonGroup>
            </div>

            {user && (
              <div className="space-y-1.5">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Show
                </p>
                <ButtonGroup className="w-full">
                  <Button
                    variant={following ? 'outline' : 'default'}
                    size="sm"
                    className="flex-1"
                    onClick={() => setFollowing(false)}
                  >
                    Everyone
                  </Button>
                  <Button
                    variant={following ? 'default' : 'outline'}
                    size="sm"
                    className="flex-1"
                    onClick={() => setFollowing(true)}
                  >
                    Following
                  </Button>
                </ButtonGroup>
              </div>
            )}
          </div>
        </aside>

        {/* Right: scrollable board */}
        <div className="min-w-0 flex-1">
          <ScrollArea className="h-[calc(100vh-12rem)] rounded-lg border">
            <LeaderboardTable
              entries={entries}
              sortBy={sortBy}
              emptyMessage={
                following
                  ? 'None of the people you follow are ranked yet.'
                  : undefined
              }
            />
          </ScrollArea>
        </div>
      </div>
    </div>
  )
}
