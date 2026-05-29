import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { Users } from 'lucide-react'
import { FollowActionButton } from './follow-action-button'
import type { UserProfile } from '@/types/profile'
import {
  removeFollowerMutation,
  userFollowersQueryOptions,
} from '@/queries/profile'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

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
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-3 rounded-lg border p-4"
          >
            <div className="bg-muted size-10 animate-pulse rounded-full" />
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
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
      {data.content.map((user) => (
        <div
          key={user.id}
          className="hover:bg-accent flex items-center gap-3 rounded-lg border p-4 transition-colors"
        >
          <Link
            to="/profile/$username"
            params={{ username: user.pseudo }}
            className="flex min-w-0 flex-1 items-center gap-3"
          >
            <Avatar className="size-10">
              <AvatarImage src={user.picture ?? undefined} alt={user.pseudo} />
              <AvatarFallback>
                {user.pseudo.slice(0, 2).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <span className="truncate font-medium">{user.pseudo}</span>
          </Link>
          {isOwner && (
            <FollowActionButton
              label="Remove"
              title={`Remove ${user.pseudo}?`}
              description={`${user.pseudo} will no longer follow you. They won't be notified, but they can follow you again.`}
              confirmLabel="Remove"
              isPending={
                removeMutation.isPending && removeMutation.variables === user.id
              }
              onConfirm={() => removeMutation.mutate(user.id)}
            />
          )}
        </div>
      ))}
    </div>
  )
}
