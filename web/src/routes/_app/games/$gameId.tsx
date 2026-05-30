import { Suspense, useEffect } from 'react'

import { createFileRoute, useRouter } from '@tanstack/react-router'
import { Play, Star } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import { ErrorPage } from '@/components/errors/error-page'
import { FriendActivitySection } from '@/components/games/friend-activity-section'
import { FriendReviewsSection } from '@/components/games/friend-reviews-section'
import { FriendWantToPlaySection } from '@/components/games/friend-want-to-play-section'
import { GameListsSection } from '@/components/games/game-lists-section'
import { PopularGameReviewsSection } from '@/components/games/popular-game-reviews-section'
import { GameQuickActions } from '@/components/games/quick-actions'
import { RatingDistributionChart } from '@/components/profile/rating-distribution-chart'
import { SimilarGamesSection } from '@/components/games/similar-games-section'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { similarGamesQueryOptions } from '@/queries/catalog'
import {
  friendReviewsQueryOptions,
  friendsActivityQueryOptions,
  friendsWantToPlayQueryOptions,
  popularGameReviewsQueryOptions,
} from '@/queries/game-social'
import { listsContainingGameQueryOptions } from '@/queries/lists'
import { apiFetch, isApiError } from '@/services/api'

export const Route = createFileRoute('/_app/games/$gameId')({
  component: RouteComponent,
  loader: async ({ params: { gameId }, context }) => {
    // We intentionally don't prefetch the social queries (friends activity, etc.)
    // here because background prefetching during SSR loses the request context
    // and causes the backend to see the request as anonymous. They will fetch
    // naturally on the client.

    // prefetch lists + similar games so SSR renders those sections
    await Promise.all([
      context.queryClient.prefetchQuery(
        listsContainingGameQueryOptions(gameId, 0, 6),
      ),
      context.queryClient.prefetchQuery(similarGamesQueryOptions(gameId)),
    ])

    const res = await apiFetch(`/api/games/${gameId}`)
    return res.json() as Promise<GameDetail>
  },
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

  useEffect(() => {
    document.title = `${game.title} — Checkpoint`
  }, [game.title])

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

      <div className="px-4 pt-10 pb-6">
        <button
          onClick={() => router.history.back()}
          className="text-muted-foreground hover:underline"
        >
          &larr; Back to catalog
        </button>

        <div className="mt-6 flex gap-8">
          {/* Cover */}
          {game.coverUrl ? (
            <img
              src={game.coverUrl}
              alt={game.title}
              className="w-64 h-auto rounded-lg object-cover shadow-md shrink-0"
            />
          ) : (
            <div className="w-64 h-80 rounded-lg bg-muted flex items-center justify-center text-muted-foreground shrink-0">
              No cover
            </div>
          )}

          {/* Info */}
          <div className="flex flex-col gap-4">
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

            {game.averageRating != null && (
              <div className="flex items-center gap-2 mt-1">
                <div className="flex items-center bg-yellow-400/10 text-yellow-600 px-3 py-1.5 rounded-md">
                  <Star className="h-5 w-5 fill-yellow-400 text-yellow-500 mr-2" />
                  <span className="text-xl font-bold font-mono">
                    {game.averageRating.toFixed(1)}
                  </span>
                  <span className="text-muted-foreground ml-1 text-sm">
                    / 5
                  </span>
                </div>
                <span className="text-muted-foreground text-sm">
                  ({game.ratingCount}{' '}
                  {game.ratingCount > 1 ? 'ratings' : 'rating'})
                </span>
              </div>
            )}

            {game.ratingCount > 0 && (
              <RatingDistributionChart
                distribution={game.ratingDistribution}
                className="max-w-xs"
              />
            )}

            {game.genres.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-muted-foreground font-semibold">
                  Genres:
                </span>
                {game.genres.map((g) => (
                  <span
                    key={g.id}
                    className="px-2 py-0.5 rounded-full bg-muted text-sm"
                  >
                    {g.name}
                  </span>
                ))}
              </div>
            )}

            {game.platforms.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-muted-foreground font-semibold">
                  Platforms:
                </span>
                {game.platforms.map((p) => (
                  <span
                    key={p.id}
                    className="px-2 py-0.5 rounded-full bg-muted text-sm"
                  >
                    {p.name}
                  </span>
                ))}
              </div>
            )}

            {game.companies.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-muted-foreground font-semibold">
                  Companies:
                </span>
                {game.companies.map((c) => (
                  <span key={c.id} className="text-sm">
                    {c.name}
                  </span>
                ))}
              </div>
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

            <div className="mt-4">
              <GameQuickActions game={game} />
            </div>
          </div>
        </div>

        {game.description && (
          <>
            <Separator className="my-6" />
            <div className="mb-10">
              <h2 className="text-xl font-semibold mb-2">About</h2>
              <p className="text-muted-foreground leading-relaxed whitespace-pre-line">
                {game.description}
              </p>
            </div>
          </>
        )}

        <Suspense fallback={null}>
          <GameListsSection gameId={game.id} />
        </Suspense>

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

        <Suspense fallback={null}>
          <SimilarGamesSection gameId={game.id} />
        </Suspense>

        <FriendActivitySection gameId={game.id} />
        <FriendWantToPlaySection gameId={game.id} />
        <FriendReviewsSection gameId={game.id} />
        <PopularGameReviewsSection gameId={game.id} />
      </div>
    </div>
  )
}
