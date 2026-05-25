import { useDeferredValue, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Gamepad2, Loader2, Search } from 'lucide-react'
import type { Game } from '@/types/game'
import { Input } from '@/components/ui/input'
import { searchGamesQueryOptions } from '@/queries/catalog'
import { cn } from '@/lib/utils'

interface ListGameSearchProps {
  onSelect: (game: Game) => void
  excludeIds: Array<string>
}

export function ListGameSearch({ onSelect, excludeIds }: ListGameSearchProps) {
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const isSearchActive = deferredQuery.length >= 2

  const {
    data: searchResults,
    isLoading: isSearching,
    isFetching: isFetchingSearch,
  } = useQuery({
    ...searchGamesQueryOptions(deferredQuery),
    enabled: isSearchActive,
  })

  const filteredResults = searchResults?.filter(
    (game) => !excludeIds.includes(game.id),
  )

  function handleSelect(game: Game) {
    onSelect(game)
    setQuery('')
  }

  return (
    <div className="relative">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
        <Input
          placeholder="Search games to add..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="pl-9 pr-9"
        />
        {isFetchingSearch && !isSearching && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <Loader2 className="size-4 animate-spin text-muted-foreground opacity-50" />
          </div>
        )}
      </div>

      {isSearchActive && (
        <div
          className={cn(
            'bg-popover text-popover-foreground absolute left-0 right-0 top-full z-50 mt-2 max-h-[250px] overflow-y-auto rounded-md border shadow-md transition-opacity duration-200',
            isFetchingSearch && !isSearching ? 'opacity-50' : 'opacity-100',
          )}
        >
          {isSearching && (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="size-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {!isSearching && !filteredResults?.length && (
            <p className="py-6 text-center text-sm text-muted-foreground">
              No games found.
            </p>
          )}

          {filteredResults && filteredResults.length > 0 && (
            <ul className="p-1">
              {filteredResults.slice(0, 8).map((game) => (
                <li key={game.id}>
                  <button
                    type="button"
                    className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-muted"
                    onClick={() => handleSelect(game)}
                  >
                    {game.coverUrl ? (
                      <img
                        src={game.coverUrl}
                        alt=""
                        className="h-10 w-7 rounded-sm object-cover"
                      />
                    ) : (
                      <div className="flex h-10 w-7 items-center justify-center rounded-sm bg-muted">
                        <Gamepad2 className="size-4 text-muted-foreground" />
                      </div>
                    )}
                    <div>
                      <p className="font-medium text-sm">{game.title}</p>
                      {game.releaseDate && (
                        <p className="text-xs text-muted-foreground">
                          {new Date(game.releaseDate).getFullYear()}
                        </p>
                      )}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
