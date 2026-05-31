import { Suspense } from 'react'

import { createFileRoute, useRouter } from '@tanstack/react-router'
import { Play, Star } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import { ErrorPage } from '@/components/errors/error-page'
import { FriendActivitySection } from '@/components/games/friend-activity-section'
import { FriendReviewsSection } from '@/components/games/friend-reviews-section'
import { FriendWantToPlaySection } from '@/components/games/friend-want-to-play-section'
import { GameListsSection } from '@/components/games/game-lists-section'
import { GameSidebarActions } from '@/components/games/game-sidebar-actions'
import { PopularGameReviewsSection } from '@/components/games/popular-game-reviews-section'
import {
  SimilarGamesSection,
  SimilarGamesSectionSkeleton,
} from '@/components/games/similar-games-section'
import { RatingDistributionChart } from '@/components/profile/rating-distribution-chart'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { seo } from '@/lib/seo'
import { similarGamesQueryOptions } from '@/queries/catalog'
import { listsContainingGameQueryOptions } from '@/queries/lists'
import { apiFetch, isApiError } from '@/services/api'

export const Route = createFileRoute('/_app/games/$gameId')({
  component: RouteComponent,
  loader: async ({ params: { gameId }, context }) => {
    // We intentionally don't prefetch the social queries (friends activity, etc.)
    // here because background prefetching during SSR loses the request context
    // and causes the backend to see the request as anonymous. They will fetch
    // naturally on the client.

    // Warm the lists + similar caches in the background so they're ready when
    // their sections mount, but do NOT await them: both render inside their own
    // <Suspense> boundaries (useSuspenseQuery) and are below the fold, so the
    // main game fetch must not be blocked behind them.
    void context.queryClient.prefetchQuery(
      listsContainingGameQueryOptions(gameId, 0, 6),
    )
    void context.queryClient.prefetchQuery(similarGamesQueryOptions(gameId))

    const res = await apiFetch(`/api/games/${gameId}`)
    return res.json() as Promise<GameDetail>
  },
  head: ({ loaderData }) => ({
    meta: seo({
      title: loaderData
        ? `${loaderData.title} — Checkpoint`
        : 'Game — Checkpoint',
    }),
  }),
  errorComponent: ({ error, reset }) => {
    if (isApiError(error) && error.status === 404) {
      return (
        <ErrorPage
          status={404}
          title="Game not found"
          message="We couldn't find this game. It may have been removed."
        />
      )
    }
    return (
      <ErrorPage
        status={isApiError(error) ? error.status : undefined}
        message={isApiError(error) ? error.message : undefined}
        onRetry={reset}
      />
    )
  },
})

function formatDuration(seconds: number | null): string {
  if (seconds == null) return 'N/A'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.round((seconds % 3600) / 60)
  if (hours === 0) return `${minutes}m`
  if (minutes === 0) return `${hours}h`
  return `${hours}h ${minutes}m`
}

function RouteComponent() {
  const game = Route.useLoaderData()
  const router = useRouter()

  const hasTimeToBeat =
    game.timeToBeatNormally != null ||
    game.timeToBeatHastily != null ||
    game.timeToBeatCompletely != null

  return (
    <div className="relative isolate max-w-7xl mx-auto">
      {/* Ambient artwork background — bound to the page container, strongly muted, fades on every edge */}
      {game.artworkUrl && (
        <div className="pointer-events-none absolute inset-x-0 -top-20 h-[28rem] -z-10 overflow-hidden">
          <img
            src={game.artworkUrl}
            alt=""
            aria-hidden
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-background/80" />
          {/* Top fade — softens the edge behind the nav */}
          <div className="absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-background to-transparent" />
          {/* Bottom fade — finishes higher than before */}
          <div className="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-background to-transparent" />
          {/* Left fade */}
          <div className="absolute inset-y-0 left-0 w-32 bg-gradient-to-r from-background to-transparent" />
          {/* Right fade */}
          <div className="absolute inset-y-0 right-0 w-32 bg-gradient-to-l from-background to-transparent" />
        </div>
      )}

      <div className="px-4 pt-24 pb-6">
        <button
          onClick={() => router.history.back()}
          className="text-muted-foreground hover:underline"
        >
          &larr; Back to catalog
        </button>

        <div className="mt-6 grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Left: Cover */}
          <div className="flex flex-col lg:col-span-1">
            {game.coverUrl ? (
              <img
                src={game.coverUrl}
                alt={game.title}
                className="w-full h-auto rounded-lg object-cover shadow-md"
              />
            ) : (
              <div className="w-full aspect-[3/4] rounded-lg bg-muted flex items-center justify-center text-muted-foreground">
                No cover
              </div>
            )}
          </div>

          {/* Middle: Info */}
          <div className="flex flex-col gap-4 lg:col-span-2">
            <h1 className="text-3xl font-bold">{game.title}</h1>

            {game.releaseDate && (
              <p className="text-muted-foreground">
                Released:{' '}
                {new Date(game.releaseDate).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </p>
            )}

            {game.trailerYoutubeId && (
              <Dialog>
                <DialogTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="self-start h-auto px-2 py-1 -ml-2 gap-1.5 text-muted-foreground hover:text-foreground"
                  >
                    <Play className="h-3.5 w-3.5 fill-current" />
                    Watch trailer
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-3xl p-0 overflow-hidden">
                  <DialogTitle className="sr-only">
                    {game.title} — Trailer
                  </DialogTitle>
                  <div className="aspect-video w-full">
                    <iframe
                      src={`https://www.youtube-nocookie.com/embed/${game.trailerYoutubeId}?autoplay=1&rel=0`}
                      title={`${game.title} trailer`}
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                      allowFullScreen
                      className="w-full h-full"
                    />
                  </div>
                </DialogContent>
              </Dialog>
            )}

            {game.description && (
              <div className="mt-2 text-sm text-foreground/90 leading-relaxed whitespace-pre-line">
                {game.description}
              </div>
            )}

            <div className="mt-4 flex flex-col gap-3">
              {game.genres.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-muted-foreground text-sm font-semibold">
                    Genres:
                  </span>
                  {game.genres.map((g) => (
                    <span
                      key={g.id}
                      className="px-2 py-0.5 rounded-full bg-muted text-xs"
                    >
                      {g.name}
                    </span>
                  ))}
                </div>
              )}

              {game.platforms.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-muted-foreground text-sm font-semibold">
                    Platforms:
                  </span>
                  {game.platforms.map((p) => (
                    <span
                      key={p.id}
                      className="px-2 py-0.5 rounded-full bg-muted text-xs"
                    >
                      {p.name}
                    </span>
                  ))}
                </div>
              )}

              {game.companies.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-muted-foreground text-sm font-semibold">
                    Companies:
                  </span>
                  {game.companies.map((c) => (
                    <span key={c.id} className="text-xs">
                      {c.name}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right: Actions */}
          <div className="flex flex-col gap-6 lg:col-span-1">
            <GameSidebarActions game={game} />

            {/* Rating section */}
            {game.ratingCount > 0 && (
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between border-b border-border pb-2 mb-2">
                  <span className="text-sm font-semibold tracking-wide text-foreground">
                    Ratings
                  </span>
                  <span className="text-sm text-muted-foreground">
                    {game.ratingCount.toLocaleString()}
                  </span>
                </div>

                <div className="flex gap-4 items-center">
                  <RatingDistributionChart
                    distribution={game.ratingDistribution}
                    className="flex-1 max-w-[200px]"
                  />
                  {game.averageRating != null && (
                    <div className="flex flex-col items-center justify-center pt-1">
                      <span className="text-4xl font-bold font-mono leading-none text-foreground">
                        {game.averageRating.toFixed(1)}
                      </span>
                      <div className="flex gap-1 text-primary mt-2">
                        {[1, 2, 3, 4, 5].map((star) => {
                          const rating = game.averageRating || 0
                          const isFull = rating >= star - 0.25
                          const isHalf =
                            rating >= star - 0.75 && rating < star - 0.25
                          return (
                            <div key={star} className="relative w-3.5 h-3.5">
                              <Star className="absolute inset-0 w-3.5 h-3.5 text-muted-foreground/30" />
                              {(isFull || isHalf) && (
                                <Star
                                  className="absolute inset-0 w-3.5 h-3.5 fill-current"
                                  style={
                                    isHalf
                                      ? { clipPath: 'inset(0 50% 0 0)' }
                                      : undefined
                                  }
                                />
                              )}
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {hasTimeToBeat && (
          <>
            <Separator className="my-6" />
            <div className="mb-10">
              <h2 className="text-xl font-semibold mb-4">Time to beat</h2>
              <dl className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="rounded-lg bg-muted/50 px-4 py-3">
                  <dt className="text-sm text-muted-foreground">Main story</dt>
                  <dd className="text-lg font-semibold font-mono">
                    {formatDuration(game.timeToBeatNormally)}
                  </dd>
                </div>
                <div className="rounded-lg bg-muted/50 px-4 py-3">
                  <dt className="text-sm text-muted-foreground">
                    Main + Extras
                  </dt>
                  <dd className="text-lg font-semibold font-mono">
                    {formatDuration(game.timeToBeatHastily)}
                  </dd>
                </div>
                <div className="rounded-lg bg-muted/50 px-4 py-3">
                  <dt className="text-sm text-muted-foreground">
                    Completionist
                  </dt>
                  <dd className="text-lg font-semibold font-mono">
                    {formatDuration(game.timeToBeatCompletely)}
                  </dd>
                </div>
              </dl>
            </div>
          </>
        )}

        <Suspense fallback={<SimilarGamesSectionSkeleton />}>
          <SimilarGamesSection gameId={game.id} />
        </Suspense>

        <FriendActivitySection gameId={game.id} />
        <FriendWantToPlaySection gameId={game.id} />
        <FriendReviewsSection gameId={game.id} />
        <PopularGameReviewsSection gameId={game.id} />

        <Suspense fallback={null}>
          <GameListsSection gameId={game.id} />
        </Suspense>
      </div>
    </div>
  )
}
