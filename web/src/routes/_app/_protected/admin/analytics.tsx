import { createFileRoute } from '@tanstack/react-router'
import { seo } from '@/lib/seo'
import { AdminComingSoon } from '@/components/admin/admin-coming-soon'
import { AdminPageHeader } from '@/components/admin/admin-page-header'

export const Route = createFileRoute('/_app/_protected/admin/analytics')({
  head: () => ({
    meta: seo({ title: 'Analytics — Admin — Checkpoint' }),
  }),
  component: AdminAnalyticsPage,
})

function AdminAnalyticsPage() {
  return (
    <>
      <AdminPageHeader
        title="Analytics"
        description="Platform activity at a glance."
      />
      <AdminComingSoon summary="Usage metrics and charts for the platform." />
    </>
  )
}
