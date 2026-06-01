import { useState } from 'react'
import { Link } from '@tanstack/react-router'
import { formatDistanceToNow } from 'date-fns'
import {
  AlignLeft,
  Bookmark,
  CheckCircle2,
  Flag,
  Heart,
  ListMusic,
  Pause,
  PlayCircle,
  Star,
  XCircle,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { FeedItem as FeedItemType } from '@/types/feed'
import type { PlayStatus } from '@/types/interaction'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { resolvePictureUrl } from '@/lib/picture'
import { useAuth } from '@/hooks/use-auth'

interface FeedItemProps {
  item: FeedItemType
}

const playStatusLabels: Record<string, string> = {
  ARE_PLAYING: 'started playing',
  COMPLETED: 'completed',
  ABANDONED: 'abandoned',
  PLANNING: 'is planning to play',
}

/** Play-status icons mirror the profile "Games" status tabs (ProfileStatusBar). */
const playStatusIcons: Partial<Record<PlayStatus, LucideIcon>> = {
  ARE_PLAYING: PlayCircle,
  PLAYED: Flag,
  COMPLETED: CheckCircle2,
  RETIRED: Pause,
  SHELVED: Bookmark,
  ABANDONED: XCircle,
}

function getActivityIcon(item: FeedItemType) {
  switch (item.type) {
    case 'PLAY': {
      const Icon = playStatusIcons[item.playStatus as PlayStatus] ?? PlayCircle
      return <Icon className="size-4" />
    }
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
  const { user } = useAuth()
  const isOwner = user?.id === item.user.id
  const [showSpoilers, setShowSpoilers] = useState(false)
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
          <span className="text-muted-foreground">{getActivityIcon(item)}</span>
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
            <Link
              to="/lists/$listId"
              params={{ listId: item.id }}
              className="truncate font-medium hover:underline"
            >
              {item.listTitle}
            </Link>
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
              {item.haveSpoilers && !showSpoilers && !isOwner ? (
                <span className="italic flex items-center gap-2">
                  Contains spoilers
                  <button
                    type="button"
                    className="not-italic text-[10px] font-medium px-2 py-0.5 border rounded hover:bg-muted transition-colors text-foreground"
                    onClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      setShowSpoilers(true)
                    }}
                  >
                    Show
                  </button>
                </span>
              ) : (
                <span className="whitespace-pre-wrap">
                  {item.reviewContent}
                </span>
              )}
            </Link>
          ) : (
            <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
              {item.haveSpoilers && !showSpoilers && !isOwner ? (
                <span className="italic flex items-center gap-2">
                  Contains spoilers
                  <button
                    type="button"
                    className="not-italic text-[10px] font-medium px-2 py-0.5 border rounded hover:bg-muted transition-colors text-foreground"
                    onClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      setShowSpoilers(true)
                    }}
                  >
                    Show
                  </button>
                </span>
              ) : (
                <span className="whitespace-pre-wrap">
                  {item.reviewContent}
                </span>
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
