import { Link, Outlet, createFileRoute } from '@tanstack/react-router'
import type { LinkProps } from '@tanstack/react-router'
import { AdminPageHeader } from '@/components/admin/admin-page-header'
import { cn } from '@/lib/utils'
import { seo } from '@/lib/seo'

const TAB_CLASS = 'rounded-md px-3 py-1.5 text-sm font-medium transition-colors'

const TABS: ReadonlyArray<{ label: string; link: LinkProps }> = [
  {
    label: 'Reports',
    link: {
      to: '/admin/moderation/reports',
      search: { page: 1, type: 'all' },
    },
  },
  {
    label: 'Reviews',
    link: {
      to: '/admin/moderation/reviews',
      search: { page: 1, reported: true },
    },
  },
]

export const Route = createFileRoute('/_app/_protected/admin/moderation')({
  head: () => ({
    meta: seo({ title: 'Moderation — Admin — Checkpoint' }),
  }),
  component: AdminModerationLayout,
})

function AdminModerationLayout() {
  return (
    <>
      <AdminPageHeader
        title="Moderation"
        description="Work through reports and remove offending content."
      />

      <nav
        aria-label="Moderation views"
        className="mb-4 flex items-center gap-1 border-b pb-2"
      >
        {TABS.map((tab) => (
          <Link
            key={tab.label}
            {...tab.link}
            activeOptions={{ exact: false, includeSearch: false }}
            activeProps={{
              className: cn('bg-accent text-accent-foreground', TAB_CLASS),
            }}
            inactiveProps={{
              className: cn(
                'text-muted-foreground hover:bg-accent/50 hover:text-foreground',
                TAB_CLASS,
              ),
            }}
          >
            {tab.label}
          </Link>
        ))}
      </nav>

      <Outlet />
    </>
  )
}
