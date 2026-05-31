import { Link } from '@tanstack/react-router'
import { UserCheck, Users } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ProfileInlineTab } from '@/components/profile/profile-tab-bar'
import { cn } from '@/lib/utils'

type SocialTab = Extract<ProfileInlineTab, 'followers' | 'following'>

interface ProfileSocialBarProps {
  username: string
  activeTab: SocialTab
  className?: string
}

const SOCIAL_TABS: ReadonlyArray<{
  value: SocialTab
  label: string
  icon: LucideIcon
}> = [
  { value: 'followers', label: 'Followers', icon: UserCheck },
  { value: 'following', label: 'Following', icon: Users },
]

const tabClass = (active: boolean) =>
  cn(
    'inline-flex h-8 shrink-0 items-center gap-1.5 rounded-md border border-transparent px-2.5 text-xs font-medium whitespace-nowrap transition-all',
    'focus-visible:outline-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
    active
      ? 'border-border bg-background text-foreground shadow-sm'
      : 'text-foreground/60 hover:text-foreground',
    '[&_svg]:size-3.5 [&_svg]:shrink-0',
  )

/**
 * Secondary tab bar for the Social section: switches between Followers and
 * Following. Lives under the primary {@link ProfileTabBar} "Social" tab.
 */
export function ProfileSocialBar({
  username,
  activeTab,
  className,
}: ProfileSocialBarProps) {
  return (
    <nav
      className={cn(
        'mb-6 flex max-w-full items-center gap-1 overflow-x-auto',
        className,
      )}
      aria-label="Social"
    >
      {SOCIAL_TABS.map(({ value, label, icon: Icon }) => {
        const active = value === activeTab
        return (
          <Link
            key={value}
            to="/profile/$username"
            params={{ username }}
            search={{ tab: value, page: 1 }}
            className={tabClass(active)}
            aria-current={active ? 'page' : undefined}
          >
            <Icon />
            {label}
          </Link>
        )
      })}
    </nav>
  )
}
