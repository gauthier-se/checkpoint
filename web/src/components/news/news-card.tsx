import { Link } from '@tanstack/react-router'
import { Newspaper } from 'lucide-react'
import type { NewsArticle } from '@/types/news'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'

interface NewsCardProps {
  article: NewsArticle
}

export function NewsCard({ article }: NewsCardProps) {
  const publishedDate = new Date(article.publishedAt).toLocaleDateString(
    'en-US',
    {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  )
  const isImported = article.source !== 'MANUAL'
  const sourceLabel = article.feedName ?? article.source

  return (
    <Link
      to="/news/$newsId"
      params={{ newsId: article.id }}
      className="group flex flex-col gap-3 rounded-lg border p-4 transition-colors hover:bg-accent"
    >
      <div className="aspect-[16/9] overflow-hidden rounded-md bg-muted">
        {article.picture ? (
          <img
            src={article.picture}
            alt={article.title}
            className="size-full object-cover"
          />
        ) : (
          <div className="flex size-full items-center justify-center">
            <Newspaper className="size-10 text-muted-foreground/40" />
          </div>
        )}
      </div>

      <div className="flex-1 space-y-1.5">
        <h3 className="font-semibold leading-tight line-clamp-2 group-hover:underline">
          {article.title}
        </h3>
        <p className="text-sm text-muted-foreground line-clamp-2">
          {article.description}
        </p>
      </div>

      <div className="flex items-center gap-2">
        {article.author ? (
          <>
            <Avatar className="size-5">
              <AvatarImage
                src={article.author.picture ?? undefined}
                alt={article.author.pseudo}
              />
              <AvatarFallback className="text-[10px]">
                {article.author.pseudo.slice(0, 2).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <span className="text-sm text-muted-foreground">
              {article.author.pseudo}
            </span>
            <span className="text-sm text-muted-foreground">·</span>
          </>
        ) : null}
        <span className="text-sm text-muted-foreground">{publishedDate}</span>
        {isImported ? (
          <Badge variant="secondary" className="ml-auto">
            {sourceLabel}
          </Badge>
        ) : null}
      </div>
    </Link>
  )
}
