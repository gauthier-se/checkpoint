import { Link, createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, ChevronRight, List } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import { Button } from '@/components/ui/button'
import { ButtonGroup } from '@/components/ui/button-group'
import { ListsGrid } from '@/components/lists/lists-grid'
import { getPageNumbers } from '@/lib/pagination'
import { listsContainingGameQueryOptions } from '@/queries/lists'
import { apiFetch } from '@/services/api'
import { seo } from '@/lib/seo'

const PAGE_SIZE = 20

type GameListsSearch = {
  page: number
}

export const Route = createFileRoute('/_app/games/$gameId_/lists')({
  component: GameListsPage,
  validateSearch: (search: Record<string, unknown>): GameListsSearch => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search: { page } }) => ({ page }),
  loader: async ({ params: { gameId }, context, deps: { page } }) => {
    const apiPage = Math.max(0, page - 1)
    const [, game] = await Promise.all([
      context.queryClient.ensureQueryData(
        listsContainingGameQueryOptions(gameId, apiPage, PAGE_SIZE),
      ),
      (async () => {
        const res = await apiFetch(`/api/games/${gameId}`)
        return res.json() as Promise<GameDetail>
      })(),
    ])
    return { game }
  },
  head: ({ loaderData }) => ({
    meta: seo({
      title: loaderData
        ? `Lists containing ${loaderData.game.title} — Checkpoint`
        : 'Lists — Checkpoint',
    }),
  }),
})

function GameListsPage() {
  const { gameId } = Route.useParams()
  const { page } = Route.useSearch()
  const { game } = Route.useLoaderData()
  const apiPage = Math.max(0, page - 1)

  const { data } = useQuery(
    listsContainingGameQueryOptions(gameId, apiPage, PAGE_SIZE),
  )

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <nav className="mb-6 flex items-center gap-1.5 text-sm text-muted-foreground">
        <Link
          to="/games/$gameId"
          params={{ gameId }}
          className="hover:text-foreground transition-colors"
        >
          {game.title}
        </Link>
        <ChevronRight className="size-3.5" />
        <span className="text-foreground font-medium">Lists</span>
      </nav>

      <div className="mb-8 flex items-center gap-3">
        <List className="size-6 text-muted-foreground" />
        <h1 className="text-3xl font-bold">{title}</h1>
        {data && (
          <span className="text-sm text-muted-foreground">
            ({data.metadata.totalElements}{' '}
            {data.metadata.totalElements === 1 ? 'list' : 'lists'})
          </span>
        )}
      </div>

      {data && data.content.length === 0 && (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <List className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            This game isn&apos;t in any list yet
          </p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <ListsGrid lists={data.content} />

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
