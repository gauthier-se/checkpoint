import { Link } from '@tanstack/react-router'
import {
  AlignLeft,
  BookOpen,
  Bookmark,
  Gamepad2,
  List,
  Tag,
  User,
  Users,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

export type ProfileTabKey =
  | 'profile'
  | 'games'
  | 'playing'
  | 'played'
  | 'completed'
  | 'retired'
  | 'shelved'
  | 'abandoned'
  | 'journal'
  | 'wishlist'
  | 'backlog'
  | 'tags'
  | 'liked'
  | 'reviews'
  | 'lists'
  | 'social'
  | 'followers'
  | 'following'
  | 'collection'

interface ProfileTabBarProps {
  username: string
  activeTab: ProfileTabKey
  /**
   * Vertical stacks the tabs into a column (used inside the Profile hero, to the
   * right of the avatar). Horizontal is the default full-width top bar used on
   * every other profile route. Both fall back to a horizontal scroller on mobile.
   */
  orientation?: 'horizontal' | 'vertical'
  className?: string
}

interface TabConfig {
  value: ProfileTabKey
  label: string
  icon: LucideIcon
}

/**
 * Primary navigation. Secondary bars keep the primary list short: the per-status
 * game tabs live under "Games" ({@link ProfileStatusBar}), Wishlist/Backlog/Liked
 * under "Collection" ({@link ProfileCollectionBar}), and Followers/Following under
 * "Social" ({@link ProfileSocialBar}).
 */
const TABS: ReadonlyArray<TabConfig> = [
  { value: 'profile', label: 'Profile', icon: User },
  { value: 'games', label: 'Games', icon: Gamepad2 },
  { value: 'collection', label: 'Collection', icon: Bookmark },
  { value: 'journal', label: 'Journal', icon: BookOpen },
  { value: 'tags', label: 'Tags', icon: Tag },
  { value: 'reviews', label: 'Reviews', icon: AlignLeft },
  { value: 'lists', label: 'Lists', icon: List },
  { value: 'social', label: 'Social', icon: Users },
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

// Status tabs that live on /profile/$username/games via ?tab=... The primary
// "Games" tab is considered active whenever any of these is the current tab.
const GAMES_TABS = new Set<ProfileTabKey>([
  'games',
  'playing',
  'played',
  'completed',
  'retired',
  'shelved',
  'abandoned',
])

// Inline tabs grouped under the primary "Social" tab via the secondary bar.
const SOCIAL_TABS = new Set<ProfileTabKey>(['followers', 'following'])

// Inline tabs grouped under the primary "Collection" tab via the secondary bar.
const COLLECTION_TABS = new Set<ProfileTabKey>(['wishlist', 'backlog', 'liked'])

const tabClass = (active: boolean, vertical: boolean) =>
  cn(
    'inline-flex h-8 shrink-0 items-center gap-1.5 rounded-md border border-transparent px-2 text-xs font-medium whitespace-nowrap transition-all',
    'focus-visible:outline-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
    vertical && 'sm:w-full sm:justify-start',
    active
      ? 'bg-primary text-primary-foreground shadow-sm'
      : 'text-foreground/60 hover:text-foreground',
    '[&_svg]:size-3.5 [&_svg]:shrink-0',
  )

/**
 * Shared tab bar for every profile-related route. Defaults to a horizontal top
 * bar; the Profile hero passes {@code orientation="vertical"} to stack the tabs
 * into a column on its right. The single "Games" tab opens
 * {@code /profile/$username/games}, "Lists" has its own route, and the rest are
 * inline ?tab= variants of the profile page.
 */
export function ProfileTabBar({
  username,
  activeTab,
  orientation = 'horizontal',
  className,
}: ProfileTabBarProps) {
  const vertical = orientation === 'vertical'

  return (
    <nav
      className={cn(
        'flex w-fit max-w-full items-center gap-1 overflow-x-auto rounded-lg bg-muted p-[3px]',
        vertical ? 'sm:flex-col sm:items-stretch sm:overflow-visible' : 'mb-6',
        className,
      )}
      aria-label="Profile sections"
    >
      {TABS.map(({ value, label, icon: Icon }) => {
        const active =
          value === activeTab ||
          (value === 'games' && GAMES_TABS.has(activeTab)) ||
          (value === 'social' && SOCIAL_TABS.has(activeTab)) ||
          (value === 'collection' && COLLECTION_TABS.has(activeTab))
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
              className={tabClass(active, vertical)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        if (value === 'games') {
          return (
            <Link
              key={value}
              to="/profile/$username/games"
              params={{ username }}
              search={{ tab: 'games', page: 1, sort: 'addedAt' }}
              className={tabClass(active, vertical)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        if (value === 'social') {
          return (
            <Link
              key={value}
              to="/profile/$username"
              params={{ username }}
              search={{ tab: 'followers', page: 1 }}
              className={tabClass(active, vertical)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        if (value === 'collection') {
          return (
            <Link
              key={value}
              to="/profile/$username"
              params={{ username }}
              search={{ tab: 'wishlist', page: 1 }}
              className={tabClass(active, vertical)}
              aria-current={active ? 'page' : undefined}
            >
              {content}
            </Link>
          )
        }

        // 'lists' — dedicated route
        return (
          <Link
            key={value}
            to="/profile/$username/lists"
            params={{ username }}
            search={{ page: 1 }}
            className={tabClass(active, vertical)}
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
export type ProfileGamesTabKey =
  | 'games'
  | 'playing'
  | 'played'
  | 'completed'
  | 'retired'
  | 'shelved'
  | 'abandoned'
