import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Users } from 'lucide-react'
import { FollowActionButton } from './follow-action-button'
import type { UserProfile } from '@/types/profile'
import {
  toggleFollowMutation,
  userFollowingQueryOptions,
} from '@/queries/profile'
import { MemberCard } from '@/components/members/member-card'

interface ProfileFollowingTabProps {
  profile: UserProfile
  page: number
  isOwner: boolean
}

export function ProfileFollowingTab({
  profile,
  page,
  isOwner,
}: ProfileFollowingTabProps) {
  const apiPage = Math.max(0, page - 1)
  const queryClient = useQueryClient()
  const { data, isLoading, isError } = useQuery(
    userFollowingQueryOptions(profile.id, apiPage),
  )

  const unfollowMutation = useMutation({
    mutationFn: (userId: string) => toggleFollowMutation(userId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['users', profile.id, 'following'],
      })
      void queryClient.invalidateQueries({
        queryKey: ['users', profile.username, 'profile'],
      })
    },
  })

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {Array.from({ length: 10 }).map((_, i) => (
          <div
            key={i}
            className="flex flex-col items-center gap-3 rounded-lg border p-5"
          >
            <div className="bg-muted size-16 animate-pulse rounded-full" />
            <div className="bg-muted h-4 w-24 animate-pulse rounded" />
          </div>
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Users className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          Unable to load following list
        </p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Users className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">
          Not following anyone yet
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {data.content.map((user) => (
        <MemberCard
          key={user.id}
          member={user}
          action={
            isOwner ? (
              <FollowActionButton
                label="Unfollow"
                title={`Unfollow ${user.pseudo}?`}
                description={`You will no longer follow ${user.pseudo}. You can follow them again at any time.`}
                confirmLabel="Unfollow"
                isPending={
                  unfollowMutation.isPending &&
                  unfollowMutation.variables === user.id
                }
                onConfirm={() => unfollowMutation.mutate(user.id)}
              />
            ) : undefined
          }
        />
      ))}
    </div>
  )
}
