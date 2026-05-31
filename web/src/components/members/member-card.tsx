import { Link, useRouter } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { UserMinus, UserPlus } from 'lucide-react'
import type { ReactNode } from 'react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { toggleFollowMutation } from '@/queries/profile'
import { useAuth } from '@/hooks/use-auth'
import { resolvePictureUrl } from '@/lib/picture'

interface MemberCardProps {
  /**
   * Minimal member shape. Level, counts and follow-state are optional so the
   * card can also render followers/following lists, whose API only returns
   * id/pseudo/picture.
   */
  member: {
    id: string
    pseudo: string
    picture: string | null
    level?: number
    followerCount?: number
    reviewCount?: number
    isFollowing?: boolean | null
  }
  /**
   * Overrides the default follow button (e.g. a Remove/Unfollow action on the
   * followers/following tabs). When provided, the built-in follow button is
   * not rendered.
   */
  action?: ReactNode
}

export function MemberCard({ member, action }: MemberCardProps) {
  const { user } = useAuth()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const queryClient = useQueryClient()
  const router = useRouter()

  const followMutation = useMutation({
    mutationFn: () => toggleFollowMutation(member.id),
    onSuccess: () => {
      // Refresh both data sources this card appears in: react-query lists (e.g.
      // the home "suggested members" useQuery) and router loaders (e.g. the
      // /members/all page, which renders from loader data, not react-query).
      void queryClient.invalidateQueries({ queryKey: ['members'] })
      void router.invalidate()
    },
  })

  const initials = member.pseudo.slice(0, 2).toUpperCase()
  const isOwnProfile = user?.id === member.id
  const canFollow = user && !isOwnProfile
  const hasStats = member.followerCount != null || member.reviewCount != null

  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border p-5 transition-colors hover:border-foreground/20 hover:bg-muted/40">
      <Link
        to="/profile/$username"
        params={{ username: member.pseudo }}
        className="flex flex-col items-center gap-3"
      >
        <Avatar className="size-16">
          <AvatarImage
            src={resolvePictureUrl(member.picture)}
            alt={member.pseudo}
          />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
        <div className="text-center">
          <p className="truncate font-medium">{member.pseudo}</p>
          {member.level != null && (
            <p className="text-muted-foreground text-sm">
              Level {member.level}
            </p>
          )}
        </div>
      </Link>
      {hasStats && (
        <div className="flex gap-4 text-sm text-muted-foreground">
          {member.followerCount != null && (
            <span>
              <span className="font-semibold text-foreground">
                {member.followerCount}
              </span>{' '}
              followers
            </span>
          )}
          {member.reviewCount != null && (
            <span>
              <span className="font-semibold text-foreground">
                {member.reviewCount}
              </span>{' '}
              reviews
            </span>
          )}
        </div>
      )}
      {action !== undefined
        ? action
        : mounted &&
          canFollow &&
          member.isFollowing !== undefined && (
            <Button
              variant={member.isFollowing ? 'outline' : 'default'}
              size="sm"
              onClick={() => followMutation.mutate()}
              disabled={followMutation.isPending}
            >
              {member.isFollowing ? (
                <>
                  <UserMinus className="mr-1 size-3.5" />
                  Unfollow
                </>
              ) : (
                <>
                  <UserPlus className="mr-1 size-3.5" />
                  Follow
                </>
              )}
            </Button>
          )}
    </div>
  )
}
