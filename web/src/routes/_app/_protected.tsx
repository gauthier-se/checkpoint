import {
  Outlet,
  createFileRoute,
  redirect,
  useNavigate,
} from '@tanstack/react-router'
import { useEffect } from 'react'
import type { User } from '@/types/user'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'

export const Route = createFileRoute('/_app/_protected')({
  beforeLoad: async ({ context, location }) => {
    let user: User | null = null
    try {
      user = await context.queryClient.ensureQueryData(authQueryOptions)
    } catch {
      // SSR auth check failed (cookie unreachable from web origin); let the
      // component fall back to a client-side redirect after hydration.
      return
    }

    if (!user) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname },
      })
    }
  },
  component: ProtectedLayout,
})

function ProtectedLayout() {
  const { user, isLoading } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!isLoading && !user) {
      void navigate({ to: '/login', search: { redirect: location.pathname } })
    }
  }, [isLoading, user, navigate])

  if (isLoading || !user) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    )
  }

  return <Outlet />
}
