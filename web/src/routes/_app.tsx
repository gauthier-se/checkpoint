import { Outlet, createFileRoute } from '@tanstack/react-router'
import { Footer } from '@/components/layout/footer'
import { Header } from '@/components/layout/header'
import { useNotificationsWebSocket } from '@/hooks/use-notifications-websocket'
import { authQueryOptions } from '@/hooks/use-auth'

export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context }) => {
    try {
      await context.queryClient.ensureQueryData(authQueryOptions)
    } catch {
      // SSR auth prefetch failed (typically cross-origin dev where the API
      // auth cookie isn't reachable from the web server). The client will
      // run the query on mount.
    }
  },
  component: AppLayout,
})

function AppLayout() {
  useNotificationsWebSocket()

  return (
    <>
      <Header />
      <Outlet />
      <Footer />
    </>
  )
}
