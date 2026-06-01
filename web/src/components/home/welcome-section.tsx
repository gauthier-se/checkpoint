import type { User } from '@/types/user'
import type { UserProfile } from '@/types/profile'

export function WelcomeSection({
  user,
  profile,
}: {
  user: User
  profile: UserProfile | undefined
}) {
  const xpPercent = profile
    ? Math.round((profile.xpPoint / profile.xpThreshold) * 100)
    : 0

  return (
    <section>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
        Welcome back, {user.username}!
      </h1>
      {profile && (
        <div className="mt-3 flex max-w-md items-center gap-3">
          <span className="text-sm font-medium text-muted-foreground">
            Level {profile.level}
          </span>
          <div className="h-2 flex-1 overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary transition-all duration-1000 ease-out"
              style={{ width: `${xpPercent}%` }}
            />
          </div>
          <span className="text-xs whitespace-nowrap text-muted-foreground">
            {profile.xpPoint}/{profile.xpThreshold} XP
          </span>
        </div>
      )}
    </section>
  )
}
