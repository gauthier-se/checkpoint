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
import { useWishlistBacklogActions } from '@/hooks/use-wishlist-backlog-actions'

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

  const { liked, toggleLike, likePending } = useWishlistBacklogActions(
    activeGame?.id ?? '',
  )

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
      <DialogContent
        className={cn(
          'max-h-[90vh] overflow-y-auto',
          step === 'search'
            ? 'sm:max-w-[500px]'
            : 'sm:max-w-[700px] md:max-w-[800px]',
        )}
      >
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
            <DialogHeader className="flex flex-row items-center space-y-0 gap-3 border-b pb-4 mb-2">
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
                <DialogDescription className="mt-0 pt-0.5">
                  — Record your playtime, dates, and thoughts.
                </DialogDescription>
              )}
            </DialogHeader>

            {isLoadingDetail && (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            )}

            {activeGame && (
              <div className="grid grid-cols-1 md:grid-cols-[160px_1fr] gap-8 mt-2">
                <div className="flex flex-col gap-3">
                  {activeGame.coverUrl ? (
                    <img
                      src={activeGame.coverUrl}
                      alt={activeGame.title}
                      className="w-full rounded-lg object-cover aspect-[3/4] shadow-sm"
                    />
                  ) : (
                    <div className="flex w-full items-center justify-center rounded-lg bg-muted aspect-[3/4] shadow-sm">
                      <Gamepad2 className="size-10 text-muted-foreground/40" />
                    </div>
                  )}
                  <div>
                    <h3 className="font-bold text-lg leading-tight">
                      {activeGame.title}
                    </h3>
                    {activeGame.releaseDate && (
                      <p className="text-sm text-muted-foreground mt-0.5">
                        {new Date(activeGame.releaseDate).getFullYear()}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex flex-col min-w-0">
                  <PlayLogForm
                    game={activeGame}
                    onCancel={handleBack}
                    onSuccess={handleClose}
                    isLiked={liked}
                    onToggleLike={toggleLike}
                    isLikePending={likePending}
                  />
                </div>
              </div>
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
