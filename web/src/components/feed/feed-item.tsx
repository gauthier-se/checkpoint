import { Link } from '@tanstack/react-router'
import { formatDistanceToNow } from 'date-fns'
import { AlignLeft, Heart, ListMusic, Play, Star } from 'lucide-react'
import type { FeedItem as FeedItemType } from '@/types/feed'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { resolvePictureUrl } from '@/lib/picture'

interface FeedItemProps {
  item: FeedItemType
}

const playStatusLabels: Record<string, string> = {
  ARE_PLAYING: 'started playing',
  COMPLETED: 'completed',
  ABANDONED: 'abandoned',
  PLANNING: 'is planning to play',
}

function getActivityIcon(type: FeedItemType['type']) {
  switch (type) {
    case 'PLAY':
      return <Play className="size-4" />
    case 'RATING':
      return <Star className="size-4" />
    case 'REVIEW':
      return <AlignLeft className="size-4" />
    case 'LIST':
      return <ListMusic className="size-4" />
    case 'LIKE_GAME':
      return <Heart className="size-4" />
  }
}

function getActivityText(item: FeedItemType) {
  switch (item.type) {
    case 'PLAY':
      return playStatusLabels[item.playStatus ?? ''] ?? 'played'
    case 'RATING':
      return `rated ${item.score != null ? (item.score / 2).toFixed(1) : '?'}/5`
    case 'REVIEW':
      return 'reviewed'
    case 'LIST':
      return 'created a list'
    case 'LIKE_GAME':
      return 'liked'
  }
}

export function FeedItem({ item }: FeedItemProps) {
  const timeAgo = formatDistanceToNow(new Date(item.createdAt), {
    addSuffix: true,
  })

  return (
    <div className="flex items-start gap-3 py-3">
      <Link
        to="/profile/$username"
        params={{ username: item.user.pseudo }}
        className="shrink-0"
      >
        <Avatar className="size-9">
          <AvatarImage
            src={resolvePictureUrl(item.user.picture)}
            alt={item.user.pseudo}
          />
          <AvatarFallback>
            {item.user.pseudo.slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
      </Link>

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5 text-sm">
          <span className="text-muted-foreground">
            {getActivityIcon(item.type)}
          </span>
          <Link
            to="/profile/$username"
            params={{ username: item.user.pseudo }}
            className="font-medium hover:underline"
          >
            {item.user.pseudo}
          </Link>
          <span className="text-muted-foreground">{getActivityText(item)}</span>
          {item.game && (
            <span className="flex min-w-0 items-baseline gap-1">
              <Link
                to="/games/$gameId"
                params={{ gameId: item.game.id }}
                className="truncate font-medium hover:underline"
              >
                {item.game.title}
              </Link>
              {item.game.releaseDate && (
                <span className="shrink-0 text-xs text-muted-foreground">
                  ({new Date(item.game.releaseDate).getFullYear()})
                </span>
              )}
            </span>
          )}
          {item.type === 'LIST' && item.listTitle && (
            <span className="truncate font-medium">{item.listTitle}</span>
          )}
        </div>

        {item.type === 'REVIEW' &&
          item.reviewContent &&
          (item.logId ? (
            <Link
              to="/plays/$id"
              params={{ id: item.logId }}
              className="mt-1 line-clamp-2 block text-sm text-muted-foreground hover:text-foreground hover:underline"
            >
              {item.haveSpoilers ? (
                <span className="italic">Contains spoilers</span>
              ) : (
                item.reviewContent
              )}
            </Link>
          ) : (
            <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
              {item.haveSpoilers ? (
                <span className="italic">Contains spoilers</span>
              ) : (
                item.reviewContent
              )}
            </p>
          ))}

        {item.type === 'LIST' && item.listGameCount != null && (
          <p className="mt-0.5 text-xs text-muted-foreground">
            {item.listGameCount} {item.listGameCount === 1 ? 'game' : 'games'}
          </p>
        )}

        <p className="mt-0.5 text-xs text-muted-foreground">{timeAgo}</p>
      </div>

      {item.game?.coverUrl && (
        <Link
          to="/games/$gameId"
          params={{ gameId: item.game.id }}
          className="shrink-0"
        >
          <img
            src={item.game.coverUrl}
            alt={item.game.title}
            className="h-14 w-10 rounded object-cover"
          />
        </Link>
      )}
    </div>
  )
}
