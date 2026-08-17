import { createFileRoute, redirect } from '@tanstack/react-router'

/** The reports queue is where moderation starts. */
export const Route = createFileRoute('/_app/_protected/admin/moderation/')({
  beforeLoad: () => {
    throw redirect({
      to: '/admin/moderation/reports',
      search: { page: 1, type: 'all' },
    })
  },
})
