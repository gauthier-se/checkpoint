import { Link } from '@tanstack/react-router'
import {
  ChartColumn,
  Gamepad2,
  Newspaper,
  ShieldAlert,
  Users,
} from 'lucide-react'
import { cn } from '@/lib/utils'

/**
 * Sections of the admin panel, in sidebar order. Each one mirrors a view of the
 * JavaFX desktop console (see `desktop/README.md`).
 */
export const ADMIN_NAV_ITEMS = [
  { to: '/admin/analytics', label: 'Analytics', icon: ChartColumn },
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/games', label: 'Games', icon: Gamepad2 },
  { to: '/admin/moderation', label: 'Moderation', icon: ShieldAlert },
  { to: '/admin/news', label: 'News', icon: Newspaper },
] as const

const ITEM_CLASS =
  'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors'

export function AdminNav() {
  return (
    <nav aria-label="Admin sections" className="flex flex-col gap-1">
      {ADMIN_NAV_ITEMS.map((item) => {
        const Icon = item.icon
        return (
          <Link
            key={item.to}
            to={item.to}
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
