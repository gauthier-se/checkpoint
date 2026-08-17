import { Suspense } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import type { AdminNewsSearchParams } from '@/types/admin'
import { AdminPageHeader } from '@/components/admin/admin-page-header'
import { AdminNewsTable } from '@/components/admin/news/admin-news-table'
import { NewsEditorDialog } from '@/components/admin/news/news-editor-dialog'
import { NewsImportPanel } from '@/components/admin/news/news-import-panel'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { seo } from '@/lib/seo'
import { adminNewsListQueryOptions } from '@/queries/admin/news'

export const Route = createFileRoute('/_app/_protected/admin/news')({
  head: () => ({
    meta: seo({ title: 'News — Admin — Checkpoint' }),
  }),
  validateSearch: (search: Record<string, unknown>): AdminNewsSearchParams => ({
    page: Math.max(1, Math.floor(Number(search.page ?? 1)) || 1),
  }),
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    void context.queryClient.prefetchQuery(adminNewsListQueryOptions(deps))
  },
  component: AdminNewsPage,
})

function AdminNewsPage() {
  return (
    <>
      <AdminPageHeader
        title="News"
        description="Draft, publish and import news articles."
        actions={
          <NewsEditorDialog trigger={<Button size="sm">New article</Button>} />
        }
      />

      <NewsImportPanel />

      <Suspense fallback={<TableSkeleton />}>
        <AdminNewsContent />
      </Suspense>
    </>
  )
}

function AdminNewsContent() {
  const search = Route.useSearch()
  const { data } = useSuspenseQuery(adminNewsListQueryOptions(search))

  return (
    <AdminNewsTable
      rows={data.content}
      metadata={data.metadata}
      search={search}
    />
  )
}

function TableSkeleton() {
  return (
    <div className="flex flex-col gap-2 rounded-lg border p-4">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-12 w-full" />
      ))}
    </div>
  )
}
