import { queryOptions } from '@tanstack/react-query'
import { ADMIN_QUERY_KEY, adminFetchJson } from './shared'
import type { AdminAnalytics } from '@/types/admin'

export const adminAnalyticsQueryKey = [ADMIN_QUERY_KEY, 'analytics'] as const

export function adminAnalyticsQueryOptions() {
  return queryOptions({
    queryKey: adminAnalyticsQueryKey,
    queryFn: () => adminFetchJson<AdminAnalytics>('/api/admin/analytics'),
    // The figures are running totals; a minute-old count is fine and this
    // aggregates several repository counts server-side.
    staleTime: 60_000,
  })
}
