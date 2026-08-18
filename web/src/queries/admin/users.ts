import { queryOptions } from '@tanstack/react-query'
import { ADMIN_QUERY_KEY, adminFetchJson } from './shared'
import type { AdminUser, AdminUserDetail, AdminUserEdit } from '@/types/admin'
import { apiFetch } from '@/services/api'

const USERS_PATH = '/api/admin/users'

export const adminUsersQueryKey = [ADMIN_QUERY_KEY, 'users'] as const

/**
 * The endpoint returns the full user list in one payload: unlike the other
 * admin listings it is not paginated, so searching and paging happen on the
 * client (see `lib/admin-users.ts`).
 */
export function adminUsersQueryOptions() {
  return queryOptions({
    queryKey: adminUsersQueryKey,
    queryFn: () => adminFetchJson<Array<AdminUser>>(USERS_PATH),
  })
}

export function adminUserQueryOptions(userId: string) {
  return queryOptions({
    queryKey: [...adminUsersQueryKey, userId],
    queryFn: () => adminFetchJson<AdminUserDetail>(`${USERS_PATH}/${userId}`),
  })
}

export async function banAdminUser(userId: string): Promise<void> {
  await apiFetch(`${USERS_PATH}/${userId}/ban`, { method: 'POST' })
}

export async function unbanAdminUser(userId: string): Promise<void> {
  await apiFetch(`${USERS_PATH}/${userId}/unban`, { method: 'POST' })
}

export async function editAdminUser(
  userId: string,
  payload: AdminUserEdit,
): Promise<AdminUserDetail> {
  const res = await apiFetch(`${USERS_PATH}/${userId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  return res.json() as Promise<AdminUserDetail>
}
