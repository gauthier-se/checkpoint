import { useDeferredValue, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, Search, UserCheck, UserPlus } from 'lucide-react'
import { StepFrame } from '../step-frame'
import type { MemberCard } from '@/types/member'
import { cn } from '@/lib/utils'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { authQueryOptions } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import {
  searchMembersQueryOptions,
  suggestedMembersQueryOptions,
} from '@/queries/members'
import { toggleFollowMutation } from '@/queries/profile'

interface FollowStepProps {
  onNext: () => void
}

export function FollowStep({ onNext }: FollowStepProps) {
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query.trim())
  const isSearching = deferredQuery.length >= 2

  const { data: suggested = [] } = useQuery(suggestedMembersQueryOptions(6))
  const {
    data: searchResults,
    isLoading: isSearchingMembers,
    isFetching: isFetchingMembers,
  } = useQuery(searchMembersQueryOptions(deferredQuery))

  const members: Array<MemberCard> = isSearching
    ? (searchResults?.content ?? [])
    : suggested

  const followMutation = useMutation({
    mutationFn: (userId: string) => toggleFollowMutation(userId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['members'] })
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
    },
  })

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('follow', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  return (
    <StepFrame
      title="Follow a few people"
      description="Get a feed going. You can always follow more from anyone's profile."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          <Button onClick={onNext}>I&apos;m done following</Button>
        </>
      }
    >
      <div className="relative">
        <Search className="text-muted-foreground absolute left-3 top-1/2 size-4 -translate-y-1/2" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search people by username..."
          className="pl-9 pr-9"
          aria-label="Search people to follow"
        />
        {isFetchingMembers && !isSearchingMembers && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <Loader2 className="size-4 animate-spin text-muted-foreground opacity-50" />
          </div>
        )}
      </div>

      {members.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          {isSearching
            ? `No one found for "${deferredQuery}".`
            : 'No suggestions yet — search above to find people to follow.'}
        </p>
      ) : (
        <div
          className={cn(
            'grid gap-3 sm:grid-cols-2 transition-opacity duration-200',
            isFetchingMembers && !isSearchingMembers
              ? 'opacity-50'
              : 'opacity-100',
          )}
        >
          {members.map((m: MemberCard) => (
            <SuggestedMember
              key={m.id}
              member={m}
              onFollow={() => followMutation.mutate(m.id)}
              isPending={followMutation.isPending}
            />
          ))}
        </div>
      )}
    </StepFrame>
  )
}

interface SuggestedMemberProps {
  member: MemberCard
  onFollow: () => void
  isPending: boolean
}

function SuggestedMember({
  member,
  onFollow,
  isPending,
}: SuggestedMemberProps) {
  const initials = member.pseudo.slice(0, 2).toUpperCase()
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border p-3">
      <div className="flex min-w-0 items-center gap-3">
        <Avatar className="size-10">
          <AvatarImage src={member.picture ?? undefined} alt={member.pseudo} />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{member.pseudo}</p>
          <p className="text-muted-foreground text-xs">Level {member.level}</p>
        </div>
      </div>
      <Button
        variant={member.isFollowing ? 'outline' : 'default'}
        size="sm"
        onClick={onFollow}
        disabled={isPending}
      >
        {member.isFollowing ? (
          <>
            <UserCheck className="mr-1 size-3.5" />
            Following
          </>
        ) : (
          <>
            <UserPlus className="mr-1 size-3.5" />
            Follow
          </>
        )}
      </Button>
    </div>
  )
}
