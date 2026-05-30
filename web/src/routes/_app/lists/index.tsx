import { useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { List, Plus, Search } from 'lucide-react'
import {
  popularListsQueryOptions,
  searchListsQueryOptions,
} from '@/queries/lists'
import { ListsGrid } from '@/components/lists/lists-grid'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/hooks/use-auth'

import { seo } from '@/lib/seo'

const POPULAR_SIZE = 5
const TRENDING_SIZE = 12

const TRENDING_CRITERIA = { page: 1, sort: 'recent' as const }

export const Route = createFileRoute('/_app/lists/')({
  head: () => ({
    meta: seo({ title: 'Lists — Checkpoint' }),
  }),
  component: RouteComponent,
  loader: async ({ context }) => {
    await Promise.all([
      context.queryClient.ensureQueryData(
        popularListsQueryOptions(0, POPULAR_SIZE),
      ),
      context.queryClient.ensureQueryData(
        searchListsQueryOptions(TRENDING_CRITERIA, TRENDING_SIZE),
      ),
    ])
  },
})

function DiscoverySection({
  title,
  action,
  children,
}: {
  title: string
  action?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <section className="my-8">
      <div className="flex items-center justify-between py-2">
        <h2 className="text-muted-foreground font-semibold">{title}</h2>
        {action}
      </div>
      <Separator />
      {children}
    </section>
  )
}

function RouteComponent() {
  const navigate = useNavigate({ from: '/lists' })
  const { user } = useAuth()
  const [searchInput, setSearchInput] = useState('')

  const { data: popularLists } = useSuspenseQuery(
    popularListsQueryOptions(0, POPULAR_SIZE),
  )
  const { data: trendingLists } = useSuspenseQuery(
    searchListsQueryOptions(TRENDING_CRITERIA, TRENDING_SIZE),
  )

  function handleSearchKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && searchInput.trim().length > 0) {
      navigate({
        to: '/lists/browse',
        search: { page: 1, q: searchInput.trim() },
      })
    }
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10 flex items-center justify-between gap-4 flex-wrap py-2">
        <h1 className="text-xl font-bold">Lists</h1>
        <div className="flex items-center gap-4">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Search lists..."
              className="pl-8"
            />
          </div>
          {user && (
            <Button asChild size="sm">
              <Link to="/lists/new">
                <Plus />
                Create a list
              </Link>
            </Button>
          )}
        </div>
      </div>

      {popularLists.content.length > 0 && (
        <DiscoverySection
          title="Popular Lists"
          action={
            <Link
              to="/lists/browse"
              search={{ page: 1, sort: 'popular' }}
              className="text-sm text-muted-foreground hover:text-foreground"
            >
              More
            </Link>
          }
        >
          <ListsGrid lists={popularLists.content} />
        </DiscoverySection>
      )}

      <DiscoverySection
        title="Trending Lists"
        action={
          <Link
            to="/lists/browse"
            search={{ page: 1 }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            More
          </Link>
        }
      >
        {trendingLists.content.length > 0 ? (
          <ListsGrid lists={trendingLists.content} />
        ) : (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <List className="text-muted-foreground size-12" />
            <p className="text-muted-foreground text-lg">No lists yet</p>
          </div>
        )}
      </DiscoverySection>
    </div>
  )
}
