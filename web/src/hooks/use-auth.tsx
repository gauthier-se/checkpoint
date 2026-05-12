import { queryOptions, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from '@tanstack/react-router'
import type { User } from '@/types/user'
import { apiFetch } from '@/services/api'
import { fetchCurrentUserServerFn } from '@/services/auth-server'

export const authQueryOptions = queryOptions({
  queryKey: ['auth', 'me'],
  queryFn: async (): Promise<User | null> => {
    if (typeof window === 'undefined') {
      return fetchCurrentUserServerFn()
    }
    const res = await apiFetch('/api/auth/me')
    if (!res.ok) return null
    return res.json()
  },
  staleTime: 5 * 60_000,
  retry: false,
})

export function useAuth() {
  const queryClient = useQueryClient()
  const router = useRouter()

  const { data: user = null, isPending } = useQuery(authQueryOptions)

  const logout = async () => {
    try {
      await apiFetch('/api/auth/logout', { method: 'POST' })
    } finally {
      queryClient.setQueryData(authQueryOptions.queryKey, null)
      await router.invalidate()
    }
  }

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: authQueryOptions.queryKey })

  return { user, isLoading: isPending, logout, invalidate } as const
}
