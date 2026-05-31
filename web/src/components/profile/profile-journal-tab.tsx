import { useQuery } from '@tanstack/react-query'
import { BookOpen, Lock } from 'lucide-react'
import type { UserProfile } from '@/types/profile'
import { userJournalQueryOptions } from '@/queries/profile'
import { JournalTimeline } from '@/components/collection/journal-timeline'
import { PaginationNav } from '@/components/shared/pagination-nav'

interface ProfileJournalTabProps {
  profile: UserProfile
  page: number
}

export function ProfileJournalTab({ profile, page }: ProfileJournalTabProps) {
  const apiPage = Math.max(0, page - 1)
  const { data, isLoading, isError } = useQuery(
    userJournalQueryOptions(profile.username, apiPage),
  )

  if (profile.isPrivate && !profile.isOwner) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Lock className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">This profile is private</p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex items-start gap-4 rounded-lg border p-4">
            <div className="h-24 w-16 animate-pulse rounded-md bg-muted" />
            <div className="flex-1 space-y-2">
              <div className="h-5 w-1/3 animate-pulse rounded bg-muted" />
              <div className="h-4 w-1/2 animate-pulse rounded bg-muted" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <BookOpen className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">Unable to load journal</p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <BookOpen className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No journal entries yet</p>
      </div>
    )
  }

  return (
    <div>
      <JournalTimeline entries={data.content} />
      <PaginationNav
        page={page}
        totalPages={data.metadata.totalPages}
        hasNext={data.metadata.hasNext}
        hasPrevious={data.metadata.hasPrevious}
        hideWhenSinglePage
        className="pt-6 pb-4"
        linkProps={(target) => ({
          to: '.',
          search: { tab: 'journal', page: target },
        })}
      />
    </div>
  )
}
