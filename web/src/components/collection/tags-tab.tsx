import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { Lock, Settings2, Tag } from 'lucide-react'
import type { LinkProps } from '@tanstack/react-router'
import { userTagGamesQueryOptions, userTagsQueryOptions } from '@/queries/tags'
import { GameDetailCard } from '@/components/games/game-detail-card'
import { EmptyState } from '@/components/collection/empty-state'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

interface TagsTabProps {
  /** Owner of the tags being browsed (pseudo). */
  username: string
  /** Whether the viewer owns this collection (enables tag management). */
  isOwner: boolean
  /** Whether the underlying profile is private (gates non-owners). */
  isPrivate?: boolean
  /** Currently selected tag, or undefined to show all tags. */
  selectedTag?: string
  /** Current page (1-based) for the selected tag's games. */
  page: number
  /** Builds the router link to select a tag chip (pass undefined to clear). */
  tagLinkProps: (tagName: string | undefined) => LinkProps
  /** Builds the router link for paginating the selected tag's games. */
  pageLinkProps: (target: number) => LinkProps
}

export function TagsTab({
  username,
  isOwner,
  isPrivate = false,
  selectedTag,
  page,
  tagLinkProps,
  pageLinkProps,
}: TagsTabProps) {
  const {
    data: tags,
    isLoading: tagsLoading,
    isError: tagsError,
  } = useQuery(userTagsQueryOptions(username))

  const apiPage = Math.max(0, page - 1)
  const gamesQuery = useQuery({
    ...userTagGamesQueryOptions(username, selectedTag ?? '', apiPage),
    enabled: Boolean(selectedTag) && !(isPrivate && !isOwner),
  })

  if (isPrivate && !isOwner) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">This profile is private</p>
      </div>
    )
  }

  if (tagsLoading) {
    return (
      <div className="flex flex-wrap gap-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="h-7 w-24 animate-pulse rounded-full bg-muted"
          />
        ))}
      </div>
    )
  }

  if (tagsError || !tags) {
    return (
      <EmptyState
        icon={<Tag className="size-12" />}
        title="Unable to load tags"
        description="Something went wrong loading tags. Please try again later."
      />
    )
  }

  if (tags.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <div className="mb-4 text-muted-foreground/40">
          <Tag className="size-12" />
        </div>
        <h3 className="text-lg font-semibold">No tags yet</h3>
        <p className="mt-1 max-w-sm text-sm text-muted-foreground">
          {isOwner
            ? 'Create tags to organize your play log entries!'
            : 'This user has no tags yet.'}
        </p>
        {isOwner && (
          <Button asChild className="mt-6 gap-1">
            <Link to="/$username/tags" params={{ username }}>
              <Settings2 className="size-4" />
              Manage tags
            </Link>
          </Button>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Tag filter chips */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Link className="inline-flex" {...tagLinkProps(undefined)}>
            <Badge
              variant={selectedTag ? 'outline' : 'default'}
              className="cursor-pointer"
            >
              All tags
            </Badge>
          </Link>
          {tags.map((tag) => (
            <Link
              key={tag.id}
              className="inline-flex"
              {...tagLinkProps(tag.name)}
            >
              <Badge
                variant={selectedTag === tag.name ? 'default' : 'secondary'}
                className="cursor-pointer gap-1"
              >
                <Tag className="size-3" />
                {tag.name}
                <span className="text-muted-foreground">
                  {tag.playLogsCount}
                </span>
              </Badge>
            </Link>
          ))}
        </div>
        {isOwner && (
          <Button asChild variant="ghost" size="sm" className="shrink-0 gap-1">
            <Link to="/$username/tags" params={{ username }}>
              <Settings2 className="size-4" />
              Manage tags
            </Link>
          </Button>
        )}
      </div>

      {/* Selected tag's games */}
      {!selectedTag ? (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Tag className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            Select a tag to see its games
          </p>
        </div>
      ) : gamesQuery.isLoading ? (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="flex flex-col gap-1.5">
              <div className="aspect-[3/4] animate-pulse rounded-md bg-muted" />
              <div className="h-3 w-3/4 animate-pulse rounded bg-muted" />
            </div>
          ))}
        </div>
      ) : gamesQuery.isError || !gamesQuery.data ? (
        <EmptyState
          icon={<Tag className="size-12" />}
          title="Unable to load games"
          description="Something went wrong loading this tag's games."
        />
      ) : gamesQuery.data.content.length === 0 ? (
        <EmptyState
          icon={<Tag className="size-12" />}
          title="No games for this tag"
          description={`No play log entries are tagged "${selectedTag}".`}
        />
      ) : (
        <>
          <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-7">
            {gamesQuery.data.content.map((entry) => (
              <GameDetailCard
                key={entry.id}
                title={entry.title}
                coverUrl={entry.coverUrl}
                releaseDate={entry.releaseDate}
                link={{ type: 'game', gameId: entry.videoGameId }}
              />
            ))}
          </div>
          <PaginationNav
            page={page}
            totalPages={gamesQuery.data.metadata.totalPages}
            hasNext={gamesQuery.data.metadata.hasNext}
            hasPrevious={gamesQuery.data.metadata.hasPrevious}
            hideWhenSinglePage
            className="pt-6 pb-4"
            linkProps={pageLinkProps}
          />
        </>
      )}
    </div>
  )
}
