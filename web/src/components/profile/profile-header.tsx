import {
  ArrowRight,
  Calendar,
  GitCompareArrows,
  Pencil,
  UserMinus,
  UserPlus,
} from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { XpProgressBar } from './xp-progress-bar'
import { BadgeGrid, hasMoreBadges } from './badge-grid'
import { FavoriteGamesSection } from './favorite-games-section'
import { RecentActivitySection } from './recent-activity-section'
import { RecentGamesSection } from './recent-games-section'
import type { UserProfile } from '@/types/profile'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { toggleFollowMutation } from '@/queries/profile'
import { useAuth } from '@/hooks/use-auth'
import { resolvePictureUrl } from '@/lib/picture'

// One full desktop row in the BadgeGrid (md:grid-cols-8).
const BADGE_PREVIEW_LIMIT = 8

interface ProfileHeaderProps {
  profile: UserProfile
}

export function ProfileHeader({ profile }: ProfileHeaderProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const isOwner = user?.username === profile.username

  const followMutation = useMutation({
    mutationFn: () => toggleFollowMutation(profile.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['users', profile.username, 'profile'],
      })
    },
  })

  const initials = profile.username.slice(0, 2).toUpperCase()
  const memberSince = new Date(profile.createdAt).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  })

  return (
    <div className="space-y-6">
      {/* Top section: Avatar + Info + Actions */}
      <div className="flex flex-col gap-6 sm:flex-row sm:items-start">
        <Avatar className="size-24 text-2xl">
          <AvatarImage
            src={resolvePictureUrl(profile.picture)}
            alt={profile.username}
          />
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>

        <div className="flex-1 space-y-3">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold">{profile.username}</h1>
            {profile.isPrivate && (
              <span className="bg-muted text-muted-foreground rounded-full px-2.5 py-0.5 text-xs font-medium">
                Private
              </span>
            )}
          </div>

          {profile.bio && (
            <p className="text-muted-foreground max-w-prose">{profile.bio}</p>
          )}

          <div className="text-muted-foreground flex items-center gap-1 text-sm">
            <Calendar className="size-4" />
            <span>Member since {memberSince}</span>
          </div>

          {/* Stats */}
          <div className="flex gap-6 text-sm">
            <div>
              <span className="font-semibold">{profile.followerCount}</span>{' '}
              <span className="text-muted-foreground">followers</span>
            </div>
            <div>
              <span className="font-semibold">{profile.followingCount}</span>{' '}
              <span className="text-muted-foreground">following</span>
            </div>
            <div>
              <span className="font-semibold">{profile.reviewCount}</span>{' '}
              <span className="text-muted-foreground">reviews</span>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-2">
          {isOwner && (
            <Button variant="outline" asChild>
              <Link to="/settings/profile">
                <Pencil className="mr-2 size-4" />
                Edit Profile
              </Link>
            </Button>
          )}
          {user && !isOwner && (
            <>
              <Button variant="outline" asChild>
                <Link
                  to="/profile/$username/compare"
                  params={{ username: profile.username }}
                  search={{ page: 1 }}
                >
                  <GitCompareArrows className="mr-2 size-4" />
                  Compare
                </Link>
              </Button>
              <Button
                variant={profile.isFollowing ? 'outline' : 'default'}
                onClick={() => followMutation.mutate()}
                disabled={followMutation.isPending}
              >
                {profile.isFollowing ? (
                  <>
                    <UserMinus className="mr-2 size-4" />
                    Unfollow
                  </>
                ) : (
                  <>
                    <UserPlus className="mr-2 size-4" />
                    Follow
                  </>
                )}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* XP Progress */}
      <XpProgressBar
        level={profile.level}
        xpPoint={profile.xpPoint}
        xpThreshold={profile.xpThreshold}
        className="max-w-md"
      />

      {/* Badges */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Badges</h2>
          {hasMoreBadges(profile.badges, BADGE_PREVIEW_LIMIT) && (
            <Link
              to="/profile/$username/badges"
              params={{ username: profile.username }}
              className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm font-medium"
            >
              See all badges
              <ArrowRight className="size-4" />
            </Link>
          )}
        </div>
        <BadgeGrid badges={profile.badges} limit={BADGE_PREVIEW_LIMIT} />
      </div>

      {/* Favorites */}
      <FavoriteGamesSection favorites={profile.favorites} isOwner={isOwner} />

      {/* Recent games preview */}
      <RecentGamesSection
        username={profile.username}
        isPrivate={profile.isPrivate}
        isOwner={isOwner}
      />

      {/* Recent activity */}
      <RecentActivitySection
        recentPlays={profile.recentPlays}
        isOwner={isOwner}
      />
    </div>
  )
}
