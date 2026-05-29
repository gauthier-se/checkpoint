import { Link } from '@tanstack/react-router'
import { Gamepad2, Heart, Lock } from 'lucide-react'
import type { GameListCard } from '@/types/list'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

interface ListCardProps {
  list: GameListCard
}

export function ListCard({ list }: ListCardProps) {
  const initials = list.authorPseudo.slice(0, 2).toUpperCase()

  return (
    <Link
      to="/lists/$listId"
      params={{ listId: list.id }}
      className="group flex flex-col gap-3 rounded-lg border p-4 transition-colors hover:bg-accent"
    >
      <div className="grid grid-cols-2 gap-1 aspect-[4/3] overflow-hidden rounded-md bg-muted">
        {list.coverUrls.slice(0, 4).map((url, i) => (
          <img key={i} src={url} alt="" className="size-full object-cover" />
        ))}
        {list.coverUrls.length === 0 && (
          <div className="col-span-2 flex items-center justify-center">
            <Gamepad2 className="size-10 text-muted-foreground/40" />
          </div>
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
              src={list.authorPicture ?? undefined}
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
