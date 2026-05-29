import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { List, Lock, Plus } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { myListsQueryOptions, userListsQueryOptions } from '@/queries/lists'
import { ListsGrid } from '@/components/lists/lists-grid'
import { PaginationNav } from '@/components/shared/pagination-nav'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/use-auth'

interface ProfileListsTabProps {
  profile: UserProfile
  page: number
}

export function ProfileListsTab({ profile, page }: ProfileListsTabProps) {
  const apiPage = Math.max(0, page - 1)
  // isOwner is stale during SSR (auth cookie unreachable from web
  // origin), so we derive ownership from useAuth — same pattern as TE-335.
  const { user } = useAuth()
  const isOwner = user?.username === profile.username
  // Owner hits /api/me/lists so private lists are included; visitors hit the
  // public per-username endpoint which omits them.
  const { data, isLoading, isError } = useQuery(
    isOwner
      ? myListsQueryOptions(apiPage)
      : userListsQueryOptions(profile.username, apiPage),
  )

  if (profile.isPrivate && !isOwner) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">This profile is private</p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 py-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="bg-muted h-64 animate-pulse rounded-lg" />
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <List className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load lists</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <List className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No lists yet</p>
        {isOwner && (
          <Button asChild size="sm">
            <Link to="/lists/new">
              <Plus />
              Create a list
            </Link>
          </Button>
        )}
      </div>
    )
  }

  return (
    <>
      {isOwner && (
        <div className="flex justify-end pb-2">
          <Button asChild size="sm">
            <Link to="/lists/new">
              <Plus />
              Create a list
            </Link>
          </Button>
        </div>
      )}
      <ListsGrid lists={data.content} />
      <PaginationNav
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
        hideWhenSinglePage
        className="mt-6 mb-10"
        linkProps={(target) => ({
          to: '/profile/$username/lists',
          params: { username: profile.username },
          search: { page: target },
        })}
      />
    </>
  )
}
