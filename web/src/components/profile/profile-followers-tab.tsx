import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Users } from 'lucide-react'
import { FollowActionButton } from './follow-action-button'
import type { UserProfile } from '@/types/profile'
import {
  removeFollowerMutation,
  userFollowersQueryOptions,
} from '@/queries/profile'
import { MemberCard } from '@/components/members/member-card'

interface ProfileFollowersTabProps {
  profile: UserProfile
  page: number
  isOwner: boolean
}

export function ProfileFollowersTab({
  profile,
  page,
  isOwner,
}: ProfileFollowersTabProps) {
  const apiPage = Math.max(0, page - 1)
  const queryClient = useQueryClient()
  const { data, isLoading, isError } = useQuery(
    userFollowersQueryOptions(profile.id, apiPage),
  )

  const removeMutation = useMutation({
    mutationFn: (followerId: string) => removeFollowerMutation(followerId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['users', profile.id, 'followers'],
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
          Unable to load followers list
        </p>
      </div>
    )
  }

  if (data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-center">
        <Users className="text-muted-foreground size-12" />
        <p className="text-muted-foreground text-lg">No followers yet</p>
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
                label="Remove"
                title={`Remove ${user.pseudo}?`}
                description={`${user.pseudo} will no longer follow you. They won't be notified, but they can follow you again.`}
                confirmLabel="Remove"
                isPending={
                  removeMutation.isPending &&
                  removeMutation.variables === user.id
                }
                onConfirm={() => removeMutation.mutate(user.id)}
              />
            ) : undefined
          }
        />
      ))}
    </div>
  )
}
