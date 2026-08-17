import { describe, expect, it } from 'vitest'
import type { AdminUser } from '@/types/admin'
import {
  filterAdminUsers,
  isBanned,
  paginateAdminUsers,
} from '@/lib/admin-users'

function makeUser(
  username: string,
  banned: boolean | null = false,
  email = `${username}@example.test`,
): AdminUser {
  return { id: `id-${username}`, username, email, banned }
}

const users: Array<AdminUser> = [
  makeUser('alpha'),
  makeUser('bravo', true),
  makeUser('charlie', null),
  makeUser('delta', false, 'delta@studio.example'),
]

describe('isBanned', () => {
  it('treats a null ban flag as active', () => {
    expect(isBanned({ banned: null })).toBe(false)
    expect(isBanned({ banned: false })).toBe(false)
    expect(isBanned({ banned: true })).toBe(true)
  })
})

describe('filterAdminUsers', () => {
  it('returns everything with no query and the "all" status', () => {
    expect(filterAdminUsers(users, { status: 'all' })).toHaveLength(4)
  })

  it('matches the username case-insensitively', () => {
    const result = filterAdminUsers(users, { q: 'BRA', status: 'all' })
    expect(result.map((user) => user.username)).toEqual(['bravo'])
  })

  it('matches the email as well as the username', () => {
    const result = filterAdminUsers(users, { q: 'studio', status: 'all' })
    expect(result.map((user) => user.username)).toEqual(['delta'])
  })

  it('ignores surrounding whitespace in the query', () => {
    expect(
      filterAdminUsers(users, { q: '  alpha  ', status: 'all' }),
    ).toHaveLength(1)
  })

  it('keeps only banned accounts for the banned status', () => {
    const result = filterAdminUsers(users, { status: 'banned' })
    expect(result.map((user) => user.username)).toEqual(['bravo'])
  })

  it('counts an unknown ban flag as active', () => {
    const result = filterAdminUsers(users, { status: 'active' })
    expect(result.map((user) => user.username)).toEqual([
      'alpha',
      'charlie',
      'delta',
    ])
  })

  it('combines the query and the status', () => {
    expect(filterAdminUsers(users, { q: 'a', status: 'banned' })).toEqual([
      users[1],
    ])
  })
})

describe('paginateAdminUsers', () => {
  const many = Array.from({ length: 25 }, (_, index) =>
    makeUser(`user-${String(index).padStart(2, '0')}`),
  )

  it('slices the requested page and reports API-shaped metadata', () => {
    const { rows, metadata } = paginateAdminUsers(many, 2, 10)

    expect(rows).toHaveLength(10)
    expect(rows[0].username).toBe('user-10')
    expect(metadata).toEqual({
      page: 1,
      size: 10,
      totalElements: 25,
      totalPages: 3,
      first: false,
      last: false,
      hasNext: true,
      hasPrevious: true,
    })
  })

  it('marks the last page correctly when it is partial', () => {
    const { rows, metadata } = paginateAdminUsers(many, 3, 10)

    expect(rows).toHaveLength(5)
    expect(metadata.last).toBe(true)
    expect(metadata.hasNext).toBe(false)
  })

  it('clamps a page beyond the end onto the last page', () => {
    const { rows, metadata } = paginateAdminUsers(many, 99, 10)

    expect(metadata.page).toBe(2)
    expect(rows[0].username).toBe('user-20')
  })

  it('clamps a page below the first onto page one', () => {
    expect(paginateAdminUsers(many, 0, 10).metadata.page).toBe(0)
    expect(paginateAdminUsers(many, NaN, 10).metadata.page).toBe(0)
  })

  it('reports a single empty page rather than zero pages', () => {
    const { rows, metadata } = paginateAdminUsers([], 1, 10)

    expect(rows).toEqual([])
    expect(metadata.totalPages).toBe(1)
    expect(metadata.totalElements).toBe(0)
    expect(metadata.first).toBe(true)
    expect(metadata.last).toBe(true)
  })
})
