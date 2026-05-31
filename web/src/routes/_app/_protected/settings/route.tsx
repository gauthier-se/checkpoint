import { Link, Outlet, createFileRoute } from '@tanstack/react-router'
import { Bell, Link2, Shield, User } from 'lucide-react'
import { cn } from '@/lib/utils'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/settings')({
  head: () => ({
    meta: seo({ title: 'Settings — Checkpoint' }),
  }),
  component: SettingsLayout,
})

const navItems = [
  { to: '/settings/profile', label: 'Profile', icon: User },
  { to: '/settings/notifications', label: 'Notifications', icon: Bell },
  { to: '/settings/security', label: 'Security', icon: Shield },
  { to: '/settings/integrations', label: 'Integrations', icon: Link2 },
] as const

function SettingsLayout() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:py-10">
      <div className="grid gap-8 md:grid-cols-[200px_1fr] lg:grid-cols-[250px_1fr]">
        <div className="flex flex-col gap-6">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
            <p className="text-sm text-muted-foreground">
              Manage your account settings and preferences
            </p>
          </div>

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
                      'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    ),
                  }}
                  inactiveProps={{
                    className: cn(
                      'text-muted-foreground hover:bg-accent/50 hover:text-foreground',
                      'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    ),
                  }}
                >
                  <Icon className="size-4" />
                  {item.label}
                </Link>
              )
            })}
          </nav>
        </div>

        <div className="flex min-w-0 max-w-3xl flex-col">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
