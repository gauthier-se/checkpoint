import { describe, expect, it, vi } from 'vitest'
import { isRedirect } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import type { User } from '@/types/user'
import { ADMIN_ROLE, isAdmin, requireAdmin } from '@/lib/admin'

function makeUser(role: string): User {
  return {
    id: 'user-1',
    username: 'alpha',
    email: 'alpha@example.com',
    role,
    bio: null,
    picture: null,
    isPrivate: false,
    twoFactorEnabled: false,
    steamId: null,
    steamDisplayName: null,
    onboardingCompletedAt: null,
    onboardingSteps: {},
  }
}

/** Minimal stand-in for the route context's query client. */
function makeQueryClient(resolve: () => Promise<User | null>): QueryClient {
  return {
    ensureQueryData: vi.fn(resolve),
  } as unknown as QueryClient
}

describe('isAdmin', () => {
  it('accepts a user carrying the admin role', () => {
    expect(isAdmin(makeUser(ADMIN_ROLE))).toBe(true)
  })

  it('rejects a regular user', () => {
    expect(isAdmin(makeUser('USER'))).toBe(false)
  })

  it('rejects the Spring Security prefixed form, which the API never returns', () => {
    expect(isAdmin(makeUser('ROLE_ADMIN'))).toBe(false)
  })

  it('rejects an absent user', () => {
    expect(isAdmin(null)).toBe(false)
    expect(isAdmin(undefined)).toBe(false)
  })
})

describe('requireAdmin', () => {
  it('lets an admin through', async () => {
    const queryClient = makeQueryClient(() =>
      Promise.resolve(makeUser(ADMIN_ROLE)),
    )

    await expect(requireAdmin(queryClient)).resolves.toBeUndefined()
  })

  it('redirects a signed-in non-admin to the home page', async () => {
    const queryClient = makeQueryClient(() => Promise.resolve(makeUser('USER')))

    const error = await requireAdmin(queryClient).then(
      () => null,
      (thrown: unknown) => thrown,
    )

    expect(isRedirect(error)).toBe(true)
    expect((error as { options: { to?: string } }).options.to).toBe('/')
  })

  it('leaves an anonymous visitor to the _protected login redirect', async () => {
    const queryClient = makeQueryClient(() => Promise.resolve(null))

    await expect(requireAdmin(queryClient)).resolves.toBeUndefined()
  })

  it('does not lock out an admin when the SSR session lookup fails', async () => {
    const queryClient = makeQueryClient(() =>
      Promise.reject(new Error('no cookie')),
    )

    await expect(requireAdmin(queryClient)).resolves.toBeUndefined()
  })
})
