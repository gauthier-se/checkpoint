import { Gamepad2, Plus } from 'lucide-react'
import { Link } from '@tanstack/react-router'
import type { FavoriteGame } from '@/types/profile'
import { Separator } from '@/components/ui/separator'

const SLOT_COUNT = 5

interface FavoriteGamesSectionProps {
  favorites: Array<FavoriteGame>
  isOwner: boolean
}

export function FavoriteGamesSection({
  favorites,
  isOwner,
}: FavoriteGamesSectionProps) {
  // Build a fixed-length array of 5 slots so the layout never collapses.
  const slots: Array<FavoriteGame | null> = Array.from(
    { length: SLOT_COUNT },
    (_, i) => favorites[i] ?? null,
  )

  return (
    <div className="space-y-3">
      <div>
        <div className="flex items-center justify-between py-2">
          <h2 className="text-muted-foreground font-semibold">Favorites</h2>
        </div>
        <Separator />
      </div>
      <div className="grid grid-cols-5 gap-2 sm:gap-3">
        {slots.map((slot, index) =>
          slot ? (
            <Link
              key={slot.gameId}
              to="/games/$gameId"
              params={{ gameId: slot.gameId }}
              title={slot.title}
              className="group rounded-md transition-opacity hover:opacity-90"
            >
              {slot.coverUrl ? (
                <img
                  src={slot.coverUrl}
                  alt={slot.title}
                  className="aspect-[3/4] w-full rounded-md object-cover"
                />
              ) : (
                <div className="bg-muted flex aspect-[3/4] w-full items-center justify-center rounded-md">
                  <Gamepad2 className="text-muted-foreground size-8" />
                </div>
              )}
            </Link>
          ) : isOwner ? (
            <Link
              key={`empty-${index}`}
              to="/settings/profile"
              hash="favorites"
              className="border-muted-foreground/30 text-muted-foreground hover:border-muted-foreground/60 hover:text-foreground flex aspect-[3/4] w-full flex-col items-center justify-center gap-1 rounded-md border border-dashed text-xs transition-colors"
            >
              <Plus className="size-5" />
              <span>Add favorite</span>
            </Link>
          ) : (
            <div
              key={`empty-${index}`}
              className="border-muted-foreground/20 aspect-[3/4] w-full rounded-md border border-dashed"
            />
          ),
        )}
      </div>
    </div>
  )
}
