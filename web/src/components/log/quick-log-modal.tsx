import { useDeferredValue, useEffect, useState } from 'react'
import { useMatch } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Gamepad2, Loader2, Search } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { PlayLogForm } from '@/components/games/play-log-form'
import { cn } from '@/lib/utils'
import {
  gameDetailQueryOptions,
  searchGamesQueryOptions,
} from '@/queries/catalog'

interface QuickLogModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

type ModalStep = 'search' | 'form'

export function QuickLogModal({ open, onOpenChange }: QuickLogModalProps) {
  const [step, setStep] = useState<ModalStep>('search')
  const [selectedGameId, setSelectedGameId] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const isSearchActive = deferredQuery.length >= 2

  const gameMatch = useMatch({
    from: '/_app/games/$gameId',
    shouldThrow: false,
  })

  const contextGame = gameMatch?.loaderData

  // Reset state when modal closes
  useEffect(() => {
    if (!open) {
      setStep('search')
      setSelectedGameId(null)
      setQuery('')
    }
  }, [open])

  const {
    data: searchResults,
    isLoading: isSearching,
    isFetching: isFetchingSearch,
  } = useQuery({
    ...searchGamesQueryOptions(deferredQuery),
    enabled: isSearchActive && step === 'search',
  })

  const gameDetailToFetch =
    step === 'form' && selectedGameId && !contextGame ? selectedGameId : null
  const { data: fetchedGame, isLoading: isLoadingDetail } = useQuery({
    ...gameDetailQueryOptions(gameDetailToFetch ?? ''),
    enabled: !!gameDetailToFetch,
  })

  const activeGame = contextGame ?? fetchedGame

  function handleGameSelect(gameId: string) {
    setSelectedGameId(gameId)
    setStep('form')
  }

  function handleBack() {
    setStep('search')
    setSelectedGameId(null)
  }

  function handleClose() {
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
        {step === 'search' && (
          <>
            <DialogHeader>
              <DialogTitle>Quick Log</DialogTitle>
              <DialogDescription>
                Search for a game to log a play session.
              </DialogDescription>
            </DialogHeader>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input
                autoFocus
                placeholder="Search games..."
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
            <div
              className={cn(
                'mt-2 max-h-[400px] overflow-y-auto transition-opacity duration-200',
                isFetchingSearch && !isSearching ? 'opacity-50' : 'opacity-100',
              )}
            >
              {isSearching && (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="size-5 animate-spin text-muted-foreground" />
                </div>
              )}

              {!isSearching && isSearchActive && !searchResults?.length && (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  No games found.
                </p>
              )}

              {!isSearchActive && !contextGame && (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  Type at least 2 characters to search...
                </p>
              )}

              {!isSearchActive && contextGame && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground px-3 py-2">
                    Current page
                  </p>
                  <button
                    type="button"
                    className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-muted"
                    onClick={() => handleGameSelect(contextGame.id)}
                  >
                    {contextGame.coverUrl ? (
                      <img
                        src={contextGame.coverUrl}
                        alt=""
                        className="h-10 w-7 rounded-sm object-cover"
                      />
                    ) : (
                      <div className="flex h-10 w-7 items-center justify-center rounded-sm bg-muted">
                        <Gamepad2 className="size-4 text-muted-foreground" />
                      </div>
                    )}
                    <div>
                      <p className="font-medium text-sm">{contextGame.title}</p>
                      {contextGame.releaseDate && (
                        <p className="text-xs text-muted-foreground">
                          {new Date(contextGame.releaseDate).getFullYear()}
                        </p>
                      )}
                    </div>
                  </button>
                </div>
              )}

              {searchResults && searchResults.length > 0 && (
                <ul className="space-y-1">
                  {searchResults.slice(0, 8).map((game) => (
                    <li key={game.id}>
                      <button
                        type="button"
                        className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-muted"
                        onClick={() => handleGameSelect(game.id)}
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
          </>
        )}

        {step === 'form' && (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleBack}
                  className="rounded-sm p-1 transition-colors hover:bg-muted"
                >
                  <ArrowLeft className="size-4" />
                </button>
                Log Play Session
              </DialogTitle>
              {activeGame && (
                <DialogDescription>
                  Record your playtime, dates, and thoughts for{' '}
                  {activeGame.title}.
                </DialogDescription>
              )}
            </DialogHeader>

            {isLoadingDetail && (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            )}

            {activeGame && (
              <div className="flex items-center gap-3 rounded-md border p-3">
                {activeGame.coverUrl ? (
                  <img
                    src={activeGame.coverUrl}
                    alt=""
                    className="h-12 w-9 rounded-sm object-cover"
                  />
                ) : (
                  <div className="flex h-12 w-9 items-center justify-center rounded-sm bg-muted">
                    <Gamepad2 className="size-4 text-muted-foreground" />
                  </div>
                )}
                <div>
                  <p className="font-medium text-sm">{activeGame.title}</p>
                  {activeGame.releaseDate && (
                    <p className="text-xs text-muted-foreground">
                      {new Date(activeGame.releaseDate).getFullYear()}
                    </p>
                  )}
                </div>
              </div>
            )}

            {activeGame && (
              <PlayLogForm
                game={activeGame}
                onCancel={handleBack}
                onSuccess={handleClose}
              />
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
