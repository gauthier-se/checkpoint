import { Link } from '@tanstack/react-router'
import {
  Archive,
  BookOpen,
  CheckCircle2,
  Gamepad2,
  Heart,
  List,
  MessageSquare,
  PlayCircle,
  Tag,
  ThumbsUp,
  User,
  UserCheck,
  Users,
  XCircle,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

export type ProfileTabKey =
  | 'profile'
  | 'games'
  | 'playing'
  | 'completed'
  | 'dropped'
  | 'journal'
  | 'wishlist'
  | 'backlog'
  | 'tags'
  | 'liked'
  | 'reviews'
  | 'lists'
  | 'followers'
  | 'following'

interface ProfileTabBarProps {
  username: string
  activeTab: ProfileTabKey
  className?: string
}

interface TabConfig {
  value: ProfileTabKey
  label: string
  icon: LucideIcon
}

const TABS: ReadonlyArray<TabConfig> = [
  { value: 'profile', label: 'Profile', icon: User },
  { value: 'games', label: 'Games', icon: Gamepad2 },
  { value: 'playing', label: 'Playing', icon: PlayCircle },
  { value: 'completed', label: 'Completed', icon: CheckCircle2 },
  { value: 'dropped', label: 'Dropped', icon: XCircle },
  { value: 'journal', label: 'Journal', icon: BookOpen },
  { value: 'wishlist', label: 'Wishlist', icon: Heart },
  { value: 'backlog', label: 'Backlog', icon: Archive },
  { value: 'tags', label: 'Tags', icon: Tag },
  { value: 'liked', label: 'Liked', icon: ThumbsUp },
  { value: 'reviews', label: 'Reviews', icon: MessageSquare },
  { value: 'lists', label: 'Lists', icon: List },
  { value: 'followers', label: 'Followers', icon: UserCheck },
  { value: 'following', label: 'Following', icon: Users },
]

// Tabs that are rendered inline on /profile/$username via ?tab=...
const PROFILE_INLINE_TABS = new Set<ProfileTabKey>([
  'profile',
  'journal',
  'wishlist',
  'backlog',
  'tags',
  'liked',
  'reviews',
  'followers',
  'following',
])

// Tabs that live on /profile/$username/games via ?tab=...
const GAMES_TABS = new Set<ProfileTabKey>([
  'games',
  'playing',
  'completed',
  'dropped',
])

const tabClass = (active: boolean) =>
  cn(
    'inline-flex h-9 shrink-0 items-center gap-1.5 rounded-md border border-transparent px-3 text-sm font-medium whitespace-nowrap transition-all',
    'focus-visible:outline-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
    active
      ? 'bg-primary text-primary-foreground shadow-sm'
      : 'text-foreground/60 hover:text-foreground',
    '[&_svg]:size-3.5 [&_svg]:shrink-0',
  )

/**
 * Shared horizontal tab bar rendered at the top of every profile-related route
 * ({@code /profile/$username}, {@code /profile/$username/games},
 * {@code /profile/$username/lists}). Each tab is a Link that triggers a real
 * navigation — Profile lives on the profile page, Games/Playing/Completed/Dropped
 * live on the games page, Lists has its own dedicated route, and the rest are
 * rendered as inline ?tab= variants of the profile page.
 */
export function ProfileTabBar({
  username,
  activeTab,
  className,
}: ProfileTabBarProps) {
  return (
    <nav
      className={cn(
        'mb-6 flex max-w-full items-center gap-1 overflow-x-auto rounded-lg bg-muted p-[3px]',
        className,
      )}
      aria-label="Profile sections"
    >
      {TABS.map(({ value, label, icon: Icon }) => {
        const active = value === activeTab
        const content = (
          <>
            <Icon />
            {label}
          </>
        )

        if (PROFILE_INLINE_TABS.has(value)) {
          return (
            <Link
              key={value}
              to="/profile/$username"
              params={{ username }}
              search={{ tab: value as ProfileInlineTab, page: 1 }}
              className={tabClass(active)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        if (GAMES_TABS.has(value)) {
          return (
            <Link
              key={value}
              to="/profile/$username/games"
              params={{ username }}
              search={{
                tab: value as ProfileGamesTabKey,
                page: 1,
                sort: 'addedAt',
              }}
              className={tabClass(active)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        // 'lists' — dedicated placeholder route
        return (
          <Link
            key={value}
            to="/profile/$username/lists"
            params={{ username }}
            className={tabClass(active)}
            aria-current={active ? 'page' : undefined}
          >
            {content}
          </Link>
        )
      })}
    </nav>
  )
}

/** Inline tabs rendered as ?tab= variants on /profile/$username. */
export type ProfileInlineTab =
  | 'profile'
  | 'journal'
  | 'wishlist'
  | 'backlog'
  | 'tags'
  | 'liked'
  | 'reviews'
  | 'followers'
  | 'following'

/** Status tabs rendered as ?tab= variants on /profile/$username/games. */
export type ProfileGamesTabKey = 'games' | 'playing' | 'completed' | 'dropped'
