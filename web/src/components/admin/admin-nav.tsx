import { Link } from '@tanstack/react-router'
import {
  ChartColumn,
  Gamepad2,
  Newspaper,
  ShieldAlert,
  Users,
} from 'lucide-react'
import type { LinkProps } from '@tanstack/react-router'
import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AdminNavItem {
  label: string
  icon: LucideIcon
  /** Full link props, so sections with required search params carry them. */
  link: LinkProps
}

/**
 * Sections of the admin panel, in sidebar order. Each one mirrors a view of the
 * JavaFX desktop console (see `desktop/README.md`).
 */
export const ADMIN_NAV_ITEMS: ReadonlyArray<AdminNavItem> = [
  { label: 'Analytics', icon: ChartColumn, link: { to: '/admin/analytics' } },
  {
    label: 'Users',
    icon: Users,
    link: { to: '/admin/users', search: { page: 1, status: 'all' } },
  },
  { label: 'Games', icon: Gamepad2, link: { to: '/admin/games' } },
  {
    label: 'Moderation',
    icon: ShieldAlert,
    link: { to: '/admin/moderation' },
  },
  { label: 'News', icon: Newspaper, link: { to: '/admin/news' } },
]

const ITEM_CLASS =
  'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors'

export function AdminNav() {
  return (
    <nav aria-label="Admin sections" className="flex flex-col gap-1">
      {ADMIN_NAV_ITEMS.map((item) => {
        const Icon = item.icon
        return (
          <Link
            key={item.label}
            {...item.link}
            activeProps={{
              className: cn('bg-accent text-accent-foreground', ITEM_CLASS),
            }}
            inactiveProps={{
              className: cn(
                'text-muted-foreground hover:bg-accent/50 hover:text-foreground',
                ITEM_CLASS,
              ),
            }}
          >
            <Icon className="size-4" />
            {item.label}
          </Link>
        )
      })}
    </nav>
  )
}
