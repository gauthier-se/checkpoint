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
import { RatingDistributionChart } from './rating-distribution-chart'
import { BadgeGrid, hasMoreBadges } from './badge-grid'
import { FavoriteGamesSection } from './favorite-games-section'
import { RecentActivitySection } from './recent-activity-section'
import { RecentGamesSection } from './recent-games-section'
import type { ReactNode } from 'react'
import type { UserProfile } from '@/types/profile'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { toggleFollowMutation } from '@/queries/profile'
import { useAuth } from '@/hooks/use-auth'
import { resolvePictureUrl } from '@/lib/picture'
import { Separator } from '@/components/ui/separator'

// Two rows in the aside BadgeGrid (xl:grid-cols-3).
const BADGE_PREVIEW_LIMIT = 6

interface ProfileHeaderProps {
  profile: UserProfile
  /** Optional navigation rendered on the right of the hero (the profile tabs). */
  nav?: ReactNode
}

export function ProfileHeader({ profile, nav }: ProfileHeaderProps) {
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
    <div className="space-y-8">
      {/* Hero: Avatar + Info + Actions */}
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
            {nav && <div className="w-full sm:ml-auto sm:w-auto">{nav}</div>}
          </div>

          {profile.bio && (
            <p className="text-muted-foreground max-w-prose">{profile.bio}</p>
          )}

          <div className="text-muted-foreground flex items-center gap-1 text-sm">
            <Calendar className="size-4" />
            <span>Member since {memberSince}</span>
          </div>

          {/* Stats + XP Progress — grouped so the bar matches the stats width */}
          <div className="w-fit space-y-2">
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
            <XpProgressBar
              level={profile.level}
              xpPoint={profile.xpPoint}
              xpThreshold={profile.xpThreshold}
            />
          </div>

          {/* Actions */}
          <div className="flex flex-wrap gap-2 pt-1">
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
      </div>

      {/* Body: main column + aside */}
      <div className="grid gap-8 xl:grid-cols-[1fr_360px]">
        <div className="min-w-0 space-y-8">
          <FavoriteGamesSection
            favorites={profile.favorites}
            isOwner={isOwner}
          />
          <RecentActivitySection
            recentPlays={profile.recentPlays}
            isOwner={isOwner}
          />
        </div>

        <aside className="space-y-8">
          {/* Badges */}
          <div className="space-y-3">
            <div>
              <div className="flex items-center justify-between py-2">
                <h2 className="text-muted-foreground font-semibold">Badges</h2>
                {hasMoreBadges(profile.badges, BADGE_PREVIEW_LIMIT) && (
                  <Link
                    to="/profile/$username/badges"
                    params={{ username: profile.username }}
                    className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm font-medium"
                  >
                    See all
                    <ArrowRight className="size-4" />
                  </Link>
                )}
              </div>
              <Separator />
            </div>
            <BadgeGrid
              badges={profile.badges}
              limit={BADGE_PREVIEW_LIMIT}
              gridClassName="grid-cols-3 sm:grid-cols-5 md:grid-cols-6 xl:grid-cols-3"
            />
          </div>

          {/* Rating distribution */}
          <div className="space-y-3">
            <div>
              <div className="flex items-center justify-between py-2">
                <h2 className="text-muted-foreground font-semibold">
                  Rating distribution
                </h2>
              </div>
              <Separator />
            </div>
            <RatingDistributionChart
              distribution={profile.ratingDistribution}
            />
          </div>
        </aside>
      </div>

      {/* Recent games — full-width row below the two-column body */}
      <RecentGamesSection
        username={profile.username}
        isPrivate={profile.isPrivate}
        isOwner={isOwner}
      />
    </div>
  )
}
