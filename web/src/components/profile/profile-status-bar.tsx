import { Link } from '@tanstack/react-router'
import {
  Bookmark,
  CheckCircle2,
  Flag,
  Gamepad2,
  Pause,
  PlayCircle,
  XCircle,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ProfileGamesTabKey } from '@/components/profile/profile-tab-bar'
import type { LibrarySort } from '@/components/collection/library-tab'
import { cn } from '@/lib/utils'

interface ProfileStatusBarProps {
  username: string
  activeTab: ProfileGamesTabKey
  sort: LibrarySort
  className?: string
}

interface StatusTabConfig {
  value: ProfileGamesTabKey
  label: string
  icon: LucideIcon
}

export const STATUS_TABS: ReadonlyArray<StatusTabConfig> = [
  { value: 'games', label: 'All', icon: Gamepad2 },
  { value: 'playing', label: 'Playing', icon: PlayCircle },
  { value: 'played', label: 'Played', icon: Flag },
  { value: 'completed', label: 'Completed', icon: CheckCircle2 },
  { value: 'retired', label: 'Retired', icon: Pause },
  { value: 'shelved', label: 'Shelved', icon: Bookmark },
  { value: 'abandoned', label: 'Abandoned', icon: XCircle },
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
 * Secondary tab bar for {@code /profile/$username/games}: filters the library by
 * play status. Lives under the primary {@link ProfileTabBar} "Games" tab so the
 * six statuses no longer clutter the main navigation.
 */
export function ProfileStatusBar({
  username,
  activeTab,
  sort,
  className,
}: ProfileStatusBarProps) {
  return (
    <nav
      className={cn(
        'mb-6 flex max-w-full items-center gap-1 overflow-x-auto',
        className,
      )}
      aria-label="Library status"
    >
      {STATUS_TABS.map(({ value, label, icon: Icon }) => {
        const active = value === activeTab
        return (
          <Link
            key={value}
            to="/profile/$username/games"
            params={{ username }}
            search={{ tab: value, page: 1, sort }}
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
