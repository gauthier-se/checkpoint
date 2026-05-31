import { useDeferredValue, useEffect, useState } from 'react'
import { Link, createFileRoute, useNavigate } from '@tanstack/react-router'
import { ArrowLeft, Search, Users, X } from 'lucide-react'
import type { MembersResponse, MembersSearchParams } from '@/types/member'
import { MemberCard } from '@/components/members/member-card'
import { MembersPagination } from '@/components/members/members-pagination'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { apiFetch } from '@/services/api'

import { seo } from '@/lib/seo'
import { parseOptionalString } from '@/lib/search-params'

const PAGE_SIZE = 20

function buildBrowseUrl(params: MembersSearchParams): string {
  const qs = new URLSearchParams()
  qs.set('page', String(params.page - 1))
  qs.set('size', String(PAGE_SIZE))
  if (params.search) qs.set('search', params.search)
  return `/api/members?${qs.toString()}`
}

export const Route = createFileRoute('/_app/members/all')({
  head: () => ({
    meta: seo({ title: 'All members — Checkpoint' }),
  }),
  component: RouteComponent,
  validateSearch: (search: Record<string, unknown>): MembersSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
    search: parseOptionalString(search.search),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps }): Promise<MembersResponse> => {
    const res = await apiFetch(buildBrowseUrl(deps))
    return res.json()
  },
})

function RouteComponent() {
  const data = Route.useLoaderData()
  const searchParams = Route.useSearch()
  const { page, search: urlSearch } = searchParams
  const navigate = useNavigate({ from: '/members/all' })

  const [inputValue, setInputValue] = useState(urlSearch ?? '')
  const deferredQuery = useDeferredValue(inputValue)

  useEffect(() => {
    const urlQ = deferredQuery.length >= 2 ? deferredQuery : undefined
    if (urlQ !== urlSearch) {
      navigate({
        search: (prev) => ({ ...prev, search: urlQ, page: 1 }),
        replace: true,
      })
    }
  }, [deferredQuery, urlSearch, navigate])

  useEffect(() => {
    if (urlSearch && urlSearch !== inputValue) {
      setInputValue(urlSearch)
    }
  }, [urlSearch])

  function clearSearch() {
    setInputValue('')
    navigate({
      search: (prev) => ({ ...prev, search: undefined, page: 1 }),
      replace: true,
    })
  }

  const totalElements = data.metadata.totalElements
  const formattedTotal = totalElements.toLocaleString()

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mt-10">
        <Link
          to="/members"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" />
          Members
        </Link>
      </div>

      <div className="mt-4 py-2 flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-foreground">All members</h1>
          <p className="text-sm text-muted-foreground">
            {formattedTotal} {totalElements === 1 ? 'member' : 'members'}
          </p>
        </div>
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Search by pseudo..."
            className="pl-8 pr-8"
          />
          {inputValue.length > 0 && (
            <button
              type="button"
              onClick={clearSearch}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
      </div>

      <Separator />

      {data.content.length > 0 ? (
        <>
          <div className="grid grid-cols-2 gap-4 py-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {data.content.map((member) => (
              <MemberCard key={member.id} member={member} />
            ))}
          </div>
          <MembersPagination
            page={page}
            totalPages={data.metadata.totalPages}
            hasNext={data.metadata.hasNext}
            hasPrevious={data.metadata.hasPrevious}
            search={searchParams}
          />
        </>
      ) : (
        <div className="flex flex-col items-center gap-3 py-12 text-center">
          <Users className="text-muted-foreground size-12" />
          <p className="text-muted-foreground text-lg">
            {urlSearch
              ? `No members found for "${urlSearch}"`
              : 'No members to display'}
          </p>
        </div>
      )}
    </div>
  )
}
