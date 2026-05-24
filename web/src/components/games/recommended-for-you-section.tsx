import type { RecommendedGame } from '@/types/game'
import { Separator } from '@/components/ui/separator'
import { GameCard } from '@/components/games/game-card'

interface RecommendedForYouSectionProps {
  games: Array<RecommendedGame> | undefined
  isLoading: boolean
}

export function RecommendedForYouSection({
  games,
  isLoading,
}: RecommendedForYouSectionProps) {
  if (isLoading || !games || games.length === 0) return null

  return (
    <section className="my-12">
      <div className="flex items-center justify-between py-2">
        <h2 className="font-semibold text-muted-foreground">
          Recommended for you
        </h2>
      </div>
      <Separator />
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3 py-4">
        {games.map((game) => (
          <div key={game.id}>
            <GameCard game={game} reason={game.reason} />
          </div>
        ))}
      </div>
    </section>
  )
}
