import { createFileRoute, redirect } from '@tanstack/react-router'

/** `/admin` has no content of its own; analytics is the panel's landing page. */
export const Route = createFileRoute('/_app/_protected/admin/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/analytics' })
  },
})
