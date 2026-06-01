import {
  MutationCache,
  QueryClient,
  defaultShouldDehydrateQuery,
} from '@tanstack/react-query'
import { createRouter } from '@tanstack/react-router'
import { setupRouterSsrQueryIntegration } from '@tanstack/react-router-ssr-query'
import { toast } from 'sonner'

import { routeTree } from './routeTree.gen'
import { isApiError } from '@/services/api'

declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: {
      // Set to `true` on a useMutation to opt out of the global error toast
      // (typically because a local `onError` already shows one).
      suppressGlobalError?: boolean
    }
  }
}

export const getRouter = () => {
  const queryClient = new QueryClient({
    mutationCache: new MutationCache({
      onError: (error, _vars, _ctx, mutation) => {
        if (mutation.options.meta?.suppressGlobalError) return
        const message = isApiError(error)
          ? error.message
          : 'Something went wrong. Please try again.'
        toast.error(message)
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: 1,
      },
      dehydrate: {
        // Exclude auth from SSR dehydration so the client always re-fetches it
        // using the browser cookie rather than trusting the server's result.
        // The server may not be able to forward the cookie (cross-origin dev) or
        // may get a transient API error, both of which would dehydrate a false
        // "logged out" state. useAuth handles the resulting hydration mismatch
        // by staying in loading state until mounted.
        shouldDehydrateQuery: (q) =>
          defaultShouldDehydrateQuery(q) && q.queryKey[0] !== 'auth',
      },
    },
  })

  const router = createRouter({
    routeTree,
    context: { queryClient },
    scrollRestoration: true,
    defaultPreloadStaleTime: 0,
  })

  setupRouterSsrQueryIntegration({ router, queryClient })

  return router
}
