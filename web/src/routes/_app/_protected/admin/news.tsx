import { createFileRoute } from '@tanstack/react-router'
import { seo } from '@/lib/seo'
import { AdminComingSoon } from '@/components/admin/admin-coming-soon'
import { AdminPageHeader } from '@/components/admin/admin-page-header'

export const Route = createFileRoute('/_app/_protected/admin/news')({
  head: () => ({
    meta: seo({ title: 'News — Admin — Checkpoint' }),
  }),
  component: AdminNewsPage,
})

function AdminNewsPage() {
  return (
    <>
      <AdminPageHeader
        title="News"
        description="Draft, publish and import news articles."
      />
      <AdminComingSoon summary="The article editor, the publish workflow and feed imports." />
    </>
  )
}
