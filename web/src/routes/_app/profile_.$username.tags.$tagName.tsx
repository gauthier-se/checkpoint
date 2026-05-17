import { Link, createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  ArrowRight,
  ChevronRight,
  Gamepad2,
  Tag,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ButtonGroup } from '@/components/ui/button-group'
import { GameCardHoverActions } from '@/components/games/game-card-hover-actions'
import { getPageNumbers } from '@/lib/pagination'
import { userTagGamesQueryOptions } from '@/queries/tags'

type TagGamesSearch = {
  page: number
}

export const Route = createFileRoute('/_app/profile_/$username/tags/$tagName')({
  component: TagGamesPage,
  validateSearch: (search: Record<string, unknown>): TagGamesSearch => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search: { page } }) => ({ page }),
  loader: async ({
    params: { username, tagName },
    context,
    deps: { page },
  }) => {
    const apiPage = Math.max(0, page - 1)
    await context.queryClient.ensureQueryData(
      userTagGamesQueryOptions(username, tagName, apiPage),
    )
  },
})

function TagGamesPage() {
  const { username, tagName } = Route.useParams()
  const { page } = Route.useSearch()
  const apiPage = Math.max(0, page - 1)

  const { data, isLoading, isError } = useQuery(
    userTagGamesQueryOptions(username, tagName, apiPage),
  )

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      {/* Breadcrumb */}
      <nav className="mb-6 flex items-center gap-1.5 text-sm text-muted-foreground">
        <Link
          to="/profile/$username"
          params={{ username }}
          className="hover:text-foreground transition-colors"
        >
          {username}
        </Link>
        <ChevronRight className="size-3.5" />
        <Link
          to="/profile/$username"
          params={{ username }}
          search={{ tab: 'tags' as any, page: 1 }}
          className="hover:text-foreground transition-colors"
        >
          Tags
        </Link>
        <ChevronRight className="size-3.5" />
        <span className="text-foreground font-medium">{tagName}</span>
      </nav>

      {/* Header */}
      <div className="mb-8 flex items-center gap-3">
        <Tag className="size-6 text-muted-foreground" />
        <h1 className="text-3xl font-bold">{tagName}</h1>
        {data && (
          <Badge variant="secondary" className="text-sm">
            {data.metadata.totalElements}{' '}
            {data.metadata.totalElements === 1 ? 'game' : 'games'}
          </Badge>
        )}
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <div className="aspect-[3/4] animate-pulse rounded-lg bg-muted" />
              <div className="h-4 w-2/3 animate-pulse rounded bg-muted" />
            </div>
          ))}
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Tag className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            Unable to load games for this tag
          </p>
        </div>
      )}

      {/* Empty */}
      {data && data.content.length === 0 && (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Tag className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            No games tagged with &quot;{tagName}&quot; yet
          </p>
        </div>
      )}

      {/* Game grid */}
      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {data.content.map((entry) => (
              <Link
                key={entry.id}
                to="/games/$gameId"
                params={{ gameId: entry.videoGameId }}
                className="group space-y-2"
              >
                <div className="relative aspect-[3/4] overflow-hidden rounded-lg bg-muted">
                  {entry.coverUrl ? (
                    <img
                      src={entry.coverUrl}
                      alt={entry.title}
                      className="h-full w-full object-cover transition-transform group-hover:scale-105"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-secondary">
                      <Gamepad2 className="size-8 text-muted-foreground" />
                    </div>
                  )}
                  <div className="pointer-events-none absolute inset-0 bg-black/70 opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
                  <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-1 px-2 text-center text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100">
                    <span className="text-sm font-semibold line-clamp-3">
                      {entry.title}
                    </span>
                    {entry.releaseDate && (
                      <span className="text-xs text-white/80">
                        {new Date(entry.releaseDate).getFullYear()}
                      </span>
                    )}
                  </div>
                  <GameCardHoverActions gameId={entry.videoGameId} />
                </div>
                <p className="text-sm font-medium leading-tight line-clamp-2 group-hover:underline">
                  {entry.title}
                </p>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {data.metadata.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-6 pb-4">
              <Link
                to="."
                search={{ page: page - 1 }}
                disabled={!data.metadata.hasPrevious}
              >
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!data.metadata.hasPrevious}
                >
                  <ArrowLeft className="size-4" />
                  Previous
                </Button>
              </Link>
              <ButtonGroup>
                {getPageNumbers(page, data.metadata.totalPages).map((p, i) =>
                  p === '...' ? (
                    <Button
                      key={`ellipsis-${i}`}
                      variant="outline"
                      size="sm"
                      disabled
                    >
                      …
                    </Button>
                  ) : (
                    <Link key={p} to="." search={{ page: p }}>
                      <Button
                        variant={p === page ? 'default' : 'outline'}
                        size="sm"
                      >
                        {p}
                      </Button>
                    </Link>
                  ),
                )}
              </ButtonGroup>
              <Link
                to="."
                search={{ page: page + 1 }}
                disabled={!data.metadata.hasNext}
              >
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!data.metadata.hasNext}
                >
                  Next
                  <ArrowRight className="size-4" />
                </Button>
              </Link>
            </div>
          )}
        </>
      )}
    </main>
  )
}
