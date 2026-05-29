import { useQuery } from '@tanstack/react-query'
import { FriendAvatarGrid } from '@/components/games/friend-avatar-grid'
import { FriendAvatarTile } from '@/components/games/friend-avatar-tile'
import { Separator } from '@/components/ui/separator'
import { friendsWantToPlayQueryOptions } from '@/queries/game-social'

interface FriendWantToPlaySectionProps {
  gameId: string
}

export function FriendWantToPlaySection({
  gameId,
}: FriendWantToPlaySectionProps) {
  const { data } = useQuery(friendsWantToPlayQueryOptions(gameId))

  if (!data || data.totalCount === 0) {
    return null
  }

  return (
    <section className="mt-8">
      <div className="flex flex-wrap items-center justify-between gap-2 py-2">
        <h2 className="text-xl font-semibold">Want to play</h2>
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          {data.wishlistCount > 0 && <span>{data.wishlistCount} wishlist</span>}
          {data.wishlistCount > 0 && data.backlogCount > 0 && (
            <span aria-hidden>·</span>
          )}
          {data.backlogCount > 0 && <span>{data.backlogCount} backlog</span>}
        </div>
      </div>
      <Separator className="my-4" />
      <FriendAvatarGrid
        title="Want to play"
        totalCount={data.friends.length}
        renderItem={(i) => {
          const f = data.friends[i]
          const tab = f.collectionType === 'WISHLIST' ? 'wishlist' : 'backlog'
          return (
            <FriendAvatarTile
              key={f.userId}
              pseudo={f.pseudo}
              picture={f.picture}
              rating={null}
              hasReview={false}
              href={`/profile/${f.pseudo}?tab=${tab}`}
              tooltip={`${f.pseudo} — ${
                f.collectionType === 'WISHLIST' ? 'Wishlist' : 'Backlog'
              }`}
            />
          )
        }}
      />
    </section>
  )
}
