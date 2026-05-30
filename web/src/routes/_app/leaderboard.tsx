import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { Trophy } from 'lucide-react'
import type { LeaderboardSortBy } from '@/types/leaderboard'
import { leaderboardQueryOptions } from '@/queries/leaderboard'
import { LeaderboardTable } from '@/components/leaderboard/leaderboard-table'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'

import { seo } from '@/lib/seo'

const LEADERBOARD_LIMIT = 50

interface LeaderboardSearch {
  sortBy: LeaderboardSortBy
}

export const Route = createFileRoute('/_app/leaderboard')({
  head: () => ({
    meta: seo({ title: 'Leaderboard — Checkpoint' }),
  }),
  component: LeaderboardPage,
  validateSearch: (search: Record<string, unknown>): LeaderboardSearch => ({
    sortBy: search.sortBy === 'level' ? 'level' : 'xp',
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ deps, context }) =>
    context.queryClient.ensureQueryData(
      leaderboardQueryOptions(deps.sortBy, LEADERBOARD_LIMIT),
    ),
})

function LeaderboardPage() {
  const { sortBy } = Route.useSearch()
  const navigate = useNavigate({ from: '/leaderboard' })
  const { data: entries } = useSuspenseQuery(
    leaderboardQueryOptions(sortBy, LEADERBOARD_LIMIT),
  )

  const setSort = (next: LeaderboardSortBy) => {
    if (next === sortBy) return
    void navigate({ search: { sortBy: next } })
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6 py-8">
      <header className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Trophy className="size-7 text-yellow-500" />
          <div>
            <h1 className="text-2xl font-bold">Leaderboard</h1>
            <p className="text-sm text-muted-foreground">
              Top {LEADERBOARD_LIMIT} players ranked by{' '}
              {sortBy === 'xp' ? 'XP' : 'level'}.
            </p>
          </div>
        </div>
        <ButtonGroup>
          <Button
            variant={sortBy === 'xp' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSort('xp')}
          >
            XP
          </Button>
          <Button
            variant={sortBy === 'level' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setSort('level')}
          >
            Level
          </Button>
        </ButtonGroup>
      </header>

      <LeaderboardTable entries={entries} sortBy={sortBy} />
    </div>
  )
}
