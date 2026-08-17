import { Link } from '@tanstack/react-router'
import type { AdminUserDetail } from '@/types/admin'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AdminUserStatusBadge } from '@/components/admin/users/admin-user-status-badge'
import { resolvePictureUrl } from '@/lib/picture'

function formatJoinDate(value: string): string {
  // The API serialises LocalDateTime without a zone, so this renders in the
  // viewer's locale as a plain calendar date.
  return new Date(value).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-lg border p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold tabular-nums">{value}</p>
    </div>
  )
}

export function AdminUserDetailCard({ user }: { user: AdminUserDetail }) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start gap-4">
          <Avatar size="lg">
            <AvatarImage
              src={resolvePictureUrl(user.picture)}
              alt={user.username}
            />
            <AvatarFallback>
              {user.username.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <CardTitle className="flex flex-wrap items-center gap-2">
              {user.username}
              <AdminUserStatusBadge banned={user.banned} />
              {user.isPrivate && (
                <span className="text-xs font-normal text-muted-foreground">
                  private profile
                </span>
              )}
            </CardTitle>
            <p className="mt-1 truncate text-sm text-muted-foreground">
              {user.email}
            </p>
            <p className="text-sm text-muted-foreground">
              Joined {formatJoinDate(user.createdAt)}
            </p>
            <Link
              to="/profile/$username"
              params={{ username: user.username }}
              search={{ tab: 'profile', page: 1 }}
              className="mt-1 inline-block text-sm underline-offset-4 hover:underline"
            >
              View public profile
            </Link>
          </div>
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Level" value={user.level ?? 0} />
          <Stat label="XP" value={user.xpPoint ?? 0} />
          <Stat label="Reviews" value={user.reviewCount} />
          <Stat label="Reports against" value={user.reportCount} />
        </div>

        <div>
          <p className="text-xs text-muted-foreground">Bio</p>
          <p className="mt-1 text-sm whitespace-pre-wrap">
            {user.bio ?? <span className="text-muted-foreground">No bio.</span>}
          </p>
        </div>
      </CardContent>
    </Card>
  )
}
