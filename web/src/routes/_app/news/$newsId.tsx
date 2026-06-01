import { Suspense } from 'react'
import { Link, createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft, ExternalLink, Newspaper } from 'lucide-react'
import { newsDetailQueryOptions } from '@/queries/news'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { resolvePictureUrl } from '@/lib/picture'
import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/news/$newsId')({
  component: RouteComponent,
  pendingComponent: NewsDetailSkeleton,
  pendingMs: 0,
  head: () => ({
    meta: seo({ title: 'News — Checkpoint' }),
  }),
  loader: ({ params: { newsId }, context }) => {
    void context.queryClient.prefetchQuery(newsDetailQueryOptions(newsId))
  },
})

function RouteComponent() {
  return (
    <Suspense fallback={<NewsDetailSkeleton />}>
      <NewsDetailContent />
    </Suspense>
  )
}

function NewsDetailContent() {
  const { newsId } = Route.useParams()
  const { data: article } = useSuspenseQuery(newsDetailQueryOptions(newsId))

  const publishedDate = new Date(article.publishedAt).toLocaleDateString(
    'en-US',
    {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
  )
  const isImported = article.source !== 'MANUAL'
  const sourceLabel = article.feedName ?? article.source

  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <Link
        to="/news"
        search={{ page: 1 }}
        className="mb-6 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Back to news
      </Link>

      <div className="grid md:grid-cols-3 gap-8 lg:gap-12 items-start">
        <div className="md:col-span-1">
          {article.picture ? (
            <img
              src={article.picture}
              alt={article.title}
              className="w-full rounded-lg object-cover aspect-[4/3] lg:aspect-[3/2]"
            />
          ) : (
            <div className="flex w-full items-center justify-center rounded-lg bg-muted aspect-[4/3] lg:aspect-[3/2]">
              <Newspaper className="size-16 text-muted-foreground/40" />
            </div>
          )}
        </div>

        <div className="md:col-span-2">
          <h1 className="text-3xl font-bold">{article.title}</h1>

          <div className="mt-4 flex items-center gap-3">
            {article.author ? (
              <>
                <Avatar className="size-8">
                  <AvatarImage
                    src={resolvePictureUrl(article.author.picture)}
                    alt={article.author.pseudo}
                  />
                  <AvatarFallback className="text-xs">
                    {article.author.pseudo.slice(0, 2).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="flex flex-col">
                  <span className="text-sm font-medium">
                    {article.author.pseudo}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {publishedDate}
                  </span>
                </div>
              </>
            ) : (
              <div className="flex flex-col">
                <span className="text-sm font-medium">{sourceLabel}</span>
                <span className="text-xs text-muted-foreground">
                  {publishedDate}
                </span>
              </div>
            )}
            {isImported ? (
              <Badge variant="secondary" className="ml-auto">
                {sourceLabel}
              </Badge>
            ) : null}
          </div>

          <Separator className="my-6" />

          {article.externalUrl ? (
            <Button asChild variant="outline" size="sm" className="mb-6">
              <a
                href={article.externalUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Read original
                <ExternalLink className="ml-1 size-3.5" />
              </a>
            </Button>
          ) : null}

          <div className="prose prose-neutral dark:prose-invert max-w-none whitespace-pre-wrap">
            {article.description}
          </div>
        </div>
      </div>
    </main>
  )
}

function NewsDetailSkeleton() {
  return (
    <main className="mx-auto max-w-7xl px-4 py-10">
      <Skeleton className="mb-6 h-4 w-28" />
      <div className="grid md:grid-cols-3 gap-8 lg:gap-12 items-start">
        <div className="md:col-span-1">
          <Skeleton className="aspect-4/3 w-full rounded-lg" />
        </div>
        <div className="md:col-span-2 space-y-4">
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-9 w-2/3" />
          <div className="flex items-center gap-3 mt-4">
            <Skeleton className="size-8 rounded-full" />
            <div className="space-y-1.5">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-32" />
            </div>
          </div>
          <Separator className="my-6" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </div>
        </div>
      </div>
    </main>
  )
}
