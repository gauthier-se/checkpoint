import { createFileRoute } from '@tanstack/react-router'
import { seo } from '@/lib/seo'
import { AdminComingSoon } from '@/components/admin/admin-coming-soon'
import { AdminPageHeader } from '@/components/admin/admin-page-header'

export const Route = createFileRoute('/_app/_protected/admin/games')({
  head: () => ({
    meta: seo({ title: 'Games — Admin — Checkpoint' }),
  }),
  component: AdminGamesPage,
})

function AdminGamesPage() {
  return (
    <>
      <AdminPageHeader
        title="Games"
        description="Curate the catalog and run imports."
      />
      <AdminComingSoon summary="Manual catalog entries, IGDB imports and bulk import jobs." />
    </>
  )
}
