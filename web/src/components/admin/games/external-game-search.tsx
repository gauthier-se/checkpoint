import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, Search } from 'lucide-react'
import { toast } from 'sonner'
import type { ExternalGame } from '@/types/admin'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  adminGamesQueryKey,
  externalGameSearchQueryOptions,
  importExternalGame,
} from '@/queries/admin/games'
import { isApiError } from '@/services/api'

export function ExternalGameSearch() {
  const queryClient = useQueryClient()
  const [input, setInput] = useState('')
  // Committed on submit rather than per keystroke: IGDB is rate limited.
  const [query, setQuery] = useState('')

  const {
    data: results,
    isFetching,
    error,
  } = useQuery(externalGameSearchQueryOptions(query))

  const importMutation = useMutation({
    mutationFn: (game: ExternalGame) => importExternalGame(game.externalId),
    onSuccess: async (_data, game) => {
      toast.success(`“${game.title}” imported into the catalog`)
      await queryClient.invalidateQueries({ queryKey: adminGamesQueryKey })
      await queryClient.invalidateQueries({ queryKey: ['games'] })
    },
  })

  // `variables` is only meaningful while a mutation is in flight.
  const importingId = importMutation.isPending
    ? importMutation.variables.externalId
    : null

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Import from IGDB</CardTitle>
        <CardDescription>
          Search the external catalog and import a title into Checkpoint.
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-4">
        <form
          className="flex gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            setQuery(input)
          }}
        >
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Search IGDB by title"
              aria-label="Search IGDB"
              className="pl-8"
            />
          </div>
          <Button type="submit" disabled={input.trim().length < 2}>
            Search
          </Button>
        </form>

        {error && (
          <p className="text-sm text-destructive">
            {isApiError(error) && error.status === 503
              ? 'IGDB is unavailable right now. Check the API credentials, then try again.'
              : 'The search failed. Try again.'}
          </p>
        )}

        {isFetching && (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        )}

        {!isFetching && results && results.length === 0 && query !== '' && (
          <p className="py-6 text-center text-sm text-muted-foreground">
            No IGDB result for “{query}”.
          </p>
        )}

        {!isFetching && results && results.length > 0 && (
          <ul className="flex flex-col gap-2">
            {results.map((game) => (
              <li
                key={game.externalId}
                className="flex items-center gap-3 rounded-lg border p-2"
              >
                {game.coverUrl ? (
                  <img
                    src={game.coverUrl}
                    alt=""
                    className="h-16 w-12 rounded object-cover"
                    loading="lazy"
                  />
                ) : (
                  <div className="h-16 w-12 rounded bg-muted" />
                )}
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{game.title}</p>
                  <p className="text-sm text-muted-foreground">
                    {game.releaseYear ?? 'Unknown year'} · IGDB{' '}
                    {game.externalId}
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={importMutation.isPending}
                  onClick={() => importMutation.mutate(game)}
                >
                  {importingId === game.externalId && (
                    <Loader2 className="size-4 animate-spin" />
                  )}
                  Import
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
