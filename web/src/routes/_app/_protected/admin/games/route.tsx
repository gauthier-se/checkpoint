import { Link, Outlet, createFileRoute } from '@tanstack/react-router'
import type { LinkProps } from '@tanstack/react-router'
import { AdminPageHeader } from '@/components/admin/admin-page-header'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { seo } from '@/lib/seo'

const TAB_CLASS = 'rounded-md px-3 py-1.5 text-sm font-medium transition-colors'

const TABS: ReadonlyArray<{ label: string; link: LinkProps }> = [
  { label: 'Catalog', link: { to: '/admin/games', search: { page: 1 } } },
  { label: 'Import', link: { to: '/admin/games/import' } },
]

export const Route = createFileRoute('/_app/_protected/admin/games')({
  head: () => ({
    meta: seo({ title: 'Games, Admin - Checkpoint' }),
  }),
  component: AdminGamesLayout,
})

function AdminGamesLayout() {
  return (
    <>
      <AdminPageHeader
        title="Games"
        description="Curate the catalog and run imports."
        actions={
          <Button asChild size="sm">
            <Link to="/admin/games/new">Add a game</Link>
          </Button>
        }
      />

      <nav
        aria-label="Catalog views"
        className="mb-4 flex items-center gap-1 border-b pb-2"
      >
        {TABS.map((tab) => (
          <Link
            key={tab.label}
            {...tab.link}
            activeOptions={{ exact: true, includeSearch: false }}
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
