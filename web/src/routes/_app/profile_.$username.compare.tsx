import { Link, createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { ChevronRight, Lock } from 'lucide-react'
import { AffinityGauge } from '@/components/profile/affinity-gauge'
import { CommonGameRow } from '@/components/profile/common-game-row'
import { ComparePagination } from '@/components/profile/compare-pagination'
import { userCompareQueryOptions } from '@/queries/profile'
import { isApiError } from '@/services/api'
import { seo } from '@/lib/seo'

type CompareSearch = {
  page: number
}

export const Route = createFileRoute('/_app/profile_/$username/compare')({
  component: CompareProfilePage,
  head: ({ params }) => ({
    meta: seo({ title: `Compare with ${params.username} — Checkpoint` }),
  }),
  validateSearch: (search: Record<string, unknown>): CompareSearch => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search: { page } }) => ({ page }),
  loader: ({ params: { username }, context, deps: { page } }) => {
    const apiPage = Math.max(0, page - 1)
    // prefetchQuery swallows errors (private/self) so they surface inline below.
    void context.queryClient.prefetchQuery(
      userCompareQueryOptions(username, apiPage),
    )
  },
})

function CompareProfilePage() {
  const { username } = Route.useParams()
  const { page } = Route.useSearch()
  const apiPage = Math.max(0, page - 1)

  const { data, isLoading, isError, error } = useQuery(
    userCompareQueryOptions(username, apiPage),
  )

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      {/* Breadcrumb */}
      <nav className="text-muted-foreground mb-6 flex items-center gap-1.5 text-sm">
        <Link
          to="/profile/$username"
          params={{ username }}
          search={{ tab: 'reviews', page: 1 }}
          className="hover:text-foreground transition-colors"
        >
          {username}
        </Link>
        <ChevronRight className="size-3.5" />
        <span className="text-foreground font-medium">Compare</span>
      </nav>

      {isLoading && <CompareSkeleton />}

      {isError && <CompareError error={error} username={username} />}

      {data && (
        <div className="grid gap-8 lg:grid-cols-[minmax(0,22rem)_1fr] lg:items-start">
          {/* Summary card — sticky sidebar on desktop, stacked on mobile */}
          <aside className="lg:sticky lg:top-6">
            <div className="flex flex-col items-center gap-4 rounded-xl border p-6 text-center">
              <AffinityGauge score={data.affinityScore} />
              <h1 className="text-xl font-bold">You &amp; @{username}</h1>

              <div className="grid w-full grid-cols-3 gap-2">
                <SummaryStat label="You" value={data.viewerLibrarySize} />
                <SummaryStat
                  label={`@${username}`}
                  value={data.targetLibrarySize}
                />
                <SummaryStat label="In common" value={data.commonGamesCount} />
              </div>

              {data.affinityScore >= 80 && (
                <p className="font-medium text-emerald-500">Great match! 🎮</p>
              )}
              {data.affinityScore <= 20 && (
                <p className="font-medium text-red-500">
                  Very different tastes
                </p>
              )}
            </div>
          </aside>

          {/* Common games */}
          <section>
            <h2 className="mb-4 text-lg font-semibold">Games in common</h2>

            {data.commonGames.content.length === 0 ? (
              <div className="text-muted-foreground rounded-lg border border-dashed p-10 text-center">
                You and @{username} don&apos;t have any games in common yet.
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  {data.commonGames.content.map((entry) => (
                    <CommonGameRow
                      key={entry.videoGameId}
                      entry={entry}
                      targetUsername={username}
                    />
                  ))}
                </div>
                <ComparePagination
                  page={page}
                  totalPages={data.commonGames.metadata.totalPages}
                  hasNext={data.commonGames.metadata.hasNext}
                  hasPrevious={data.commonGames.metadata.hasPrevious}
                />
              </>
            )}
          </section>
        </div>
      )}
    </main>
  )
}

function SummaryStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-muted/50 rounded-lg p-3">
      <p className="text-lg font-bold">{value}</p>
      <p className="text-muted-foreground truncate text-xs">{label}</p>
    </div>
  )
}

function CompareSkeleton() {
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,22rem)_1fr] lg:items-start">
      <div className="flex flex-col items-center gap-4 rounded-xl border p-6">
        <div className="bg-muted size-32 animate-pulse rounded-full" />
        <div className="bg-muted h-6 w-40 animate-pulse rounded" />
        <div className="grid w-full grid-cols-3 gap-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="bg-muted h-16 animate-pulse rounded-lg" />
          ))}
        </div>
      </div>
      <div className="space-y-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="bg-muted h-20 animate-pulse rounded-lg" />
        ))}
      </div>
    </div>
  )
}

function CompareError({
  error,
  username,
}: {
  error: unknown
  username: string
}) {
  const status = isApiError(error) ? error.status : undefined

  if (status === 403) {
    return (
      <div className="flex flex-col items-center gap-3 py-16 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          @{username}&apos;s profile is private
        </p>
        <Link
          to="/profile/$username"
          params={{ username }}
          search={{ tab: 'reviews', page: 1 }}
          className="text-primary text-sm hover:underline"
        >
          Back to profile
        </Link>
      </div>
    )
  }

  const message =
    status === 400
      ? "You can't compare with yourself"
      : 'Unable to load this comparison'

  return (
    <div className="text-muted-foreground rounded-lg border border-dashed p-10 text-center">
      {message}
    </div>
  )
}
