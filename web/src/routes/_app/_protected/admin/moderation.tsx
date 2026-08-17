import { createFileRoute } from '@tanstack/react-router'
import { seo } from '@/lib/seo'
import { AdminComingSoon } from '@/components/admin/admin-coming-soon'
import { AdminPageHeader } from '@/components/admin/admin-page-header'

export const Route = createFileRoute('/_app/_protected/admin/moderation')({
  head: () => ({
    meta: seo({ title: 'Moderation — Admin — Checkpoint' }),
  }),
  component: AdminModerationPage,
})

function AdminModerationPage() {
  return (
    <>
      <AdminPageHeader
        title="Moderation"
        description="Work through reports and remove offending content."
      />
      <AdminComingSoon summary="The reports queue, reported reviews and comment removal." />
    </>
  )
}
