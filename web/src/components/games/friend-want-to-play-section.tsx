import { useQuery } from '@tanstack/react-query'
import { FriendAvatarGrid } from '@/components/games/friend-avatar-grid'
import { FriendAvatarTile } from '@/components/games/friend-avatar-tile'
import { DiscoverySection } from '@/components/games/discovery-section'
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
    <DiscoverySection
      title="Want to play"
      action={
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          {data.wishlistCount > 0 && <span>{data.wishlistCount} wishlist</span>}
          {data.wishlistCount > 0 && data.backlogCount > 0 && (
            <span aria-hidden>·</span>
          )}
          {data.backlogCount > 0 && <span>{data.backlogCount} backlog</span>}
        </div>
      }
    >
      <div className="pt-4">
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
                href={`/profile/${f.pseudo}/games?tab=${tab}`}
                tooltip={`${f.pseudo} — ${
                  f.collectionType === 'WISHLIST' ? 'Wishlist' : 'Backlog'
                }`}
              />
            )
          }}
        />
      </div>
    </DiscoverySection>
  )
}
