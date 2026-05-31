import { Link } from '@tanstack/react-router'
import { Gamepad2, Heart, Lock } from 'lucide-react'
import type { GameListCard } from '@/types/list'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { resolvePictureUrl } from '@/lib/picture'
import { cn } from '@/lib/utils'

interface ListCardProps {
  list: GameListCard
}

export function ListCard({ list }: ListCardProps) {
  const initials = list.authorPseudo.slice(0, 2).toUpperCase()
  const covers = list.coverUrls.slice(0, 4)

  return (
    <Link
      to="/lists/$listId"
      params={{ listId: list.id }}
      className="group flex flex-col gap-3 rounded-lg border p-4 transition-colors hover:border-foreground/20 hover:bg-muted/40"
    >
      <div className="flex aspect-[3/2] items-center justify-center overflow-hidden rounded-md bg-muted">
        {covers.length > 0 ? (
          covers.map((url, i) => (
            <img
              key={i}
              src={url}
              alt=""
              style={{ zIndex: covers.length - i }}
              className={cn(
                'aspect-[3/4] h-full object-cover shadow-md ring-1 ring-black/15 transition-[margin] duration-200',
                i !== 0 && '-ml-[24%] group-hover:-ml-[19%]',
              )}
            />
          ))
        ) : (
          <Gamepad2 className="size-10 text-muted-foreground/40" />
        )}
      </div>

      <div className="flex-1 space-y-1.5">
        <h3 className="flex items-center gap-1.5 font-semibold leading-tight line-clamp-1 group-hover:underline">
          {list.isPrivate && (
            <Lock
              className="size-3.5 shrink-0 text-muted-foreground"
              aria-label="Private list"
            />
          )}
          {list.title}
        </h3>
        {list.description && (
          <p className="text-sm text-muted-foreground line-clamp-2">
            {list.description}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Avatar className="size-5">
            <AvatarImage
              src={resolvePictureUrl(list.authorPicture)}
              alt={list.authorPseudo}
            />
            <AvatarFallback className="text-[10px]">{initials}</AvatarFallback>
          </Avatar>
          <span className="text-sm text-muted-foreground">
            {list.authorPseudo}
          </span>
        </div>
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <span className="flex items-center gap-1">
            <Gamepad2 className="size-3.5" />
            {list.videoGamesCount}
          </span>
          <span className="flex items-center gap-1">
            <Heart className="size-3.5" />
            {list.likesCount}
          </span>
        </div>
      </div>
    </Link>
  )
}
