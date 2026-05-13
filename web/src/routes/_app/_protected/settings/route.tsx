import { Link, Outlet, createFileRoute } from '@tanstack/react-router'
import { Bell, Link2, Palette, Shield, User } from 'lucide-react'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/_app/_protected/settings')({
  component: SettingsLayout,
})

const navItems = [
  { to: '/settings/profile', label: 'Profile', icon: User },
  { to: '/settings/notifications', label: 'Notifications', icon: Bell },
  { to: '/settings/security', label: 'Security', icon: Shield },
  { to: '/settings/integrations', label: 'Integrations', icon: Link2 },
  { to: '/settings/appearance', label: 'Appearance', icon: Palette },
] as const

function SettingsLayout() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground text-sm">
          Manage your account settings and preferences
        </p>
      </div>

      <div className="grid gap-8 md:grid-cols-[200px_1fr]">
        <nav className="flex flex-col gap-1">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <Link
                key={item.to}
                to={item.to}
                activeProps={{
                  className: cn(
                    'bg-accent text-accent-foreground',
                    'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium',
                  ),
                }}
                inactiveProps={{
                  className: cn(
                    'text-muted-foreground hover:bg-accent/50 hover:text-foreground',
                    'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium',
                  ),
                }}
              >
                <Icon className="size-4" />
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div>
          <Outlet />
        </div>
      </div>
    </div>
  )
}
