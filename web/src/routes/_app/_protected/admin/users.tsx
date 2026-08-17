import { createFileRoute } from '@tanstack/react-router'
import { seo } from '@/lib/seo'
import { AdminComingSoon } from '@/components/admin/admin-coming-soon'
import { AdminPageHeader } from '@/components/admin/admin-page-header'

export const Route = createFileRoute('/_app/_protected/admin/users')({
  head: () => ({
    meta: seo({ title: 'Users — Admin — Checkpoint' }),
  }),
  component: AdminUsersPage,
})

function AdminUsersPage() {
  return (
    <>
      <AdminPageHeader
        title="Users"
        description="Search accounts, review their history, ban and unban."
      />
      <AdminComingSoon summary="Account lookup, moderation history, bans and profile edits." />
    </>
  )
}
