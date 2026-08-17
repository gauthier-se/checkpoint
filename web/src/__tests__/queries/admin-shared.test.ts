import { describe, expect, it } from 'vitest'
import { ADMIN_PAGE_SIZE } from '@/types/admin'
import { buildAdminPageUrl } from '@/queries/admin/shared'

function paramsOf(url: string): URLSearchParams {
  return new URLSearchParams(url.slice(url.indexOf('?')))
}

describe('buildAdminPageUrl', () => {
  it('converts the 1-based UI page to the API 0-based page', () => {
    expect(
      paramsOf(buildAdminPageUrl('/api/admin/reports', { page: 3 })).get(
        'page',
      ),
    ).toBe('2')
  })

  it('defaults the page size to the shared admin page size', () => {
    expect(
      paramsOf(buildAdminPageUrl('/api/admin/reports', { page: 1 })).get(
        'size',
      ),
    ).toBe(String(ADMIN_PAGE_SIZE))
  })

  it('keeps the path and honours an explicit size and sort', () => {
    const url = buildAdminPageUrl('/api/admin/reviews', {
      page: 2,
      size: 5,
      sort: 'createdAt,desc',
    })

    expect(url.startsWith('/api/admin/reviews?')).toBe(true)

    const params = paramsOf(url)
    expect(params.get('page')).toBe('1')
    expect(params.get('size')).toBe('5')
    expect(params.get('sort')).toBe('createdAt,desc')
  })

  it('omits sort when it is not set', () => {
    expect(
      paramsOf(buildAdminPageUrl('/api/admin/news', { page: 1 })).has('sort'),
    ).toBe(false)
  })

  it('appends extra filters, skipping undefined and empty values', () => {
    const params = paramsOf(
      buildAdminPageUrl(
        '/api/admin/reports',
        { page: 1 },
        { type: 'review', status: undefined, q: '', limit: 10, banned: false },
      ),
    )

    expect(params.get('type')).toBe('review')
    expect(params.get('limit')).toBe('10')
    expect(params.get('banned')).toBe('false')
    expect(params.has('status')).toBe(false)
    expect(params.has('q')).toBe(false)
  })

  it('clamps a page below the first one instead of sending a negative page', () => {
    expect(
      paramsOf(buildAdminPageUrl('/api/admin/users', { page: 0 })).get('page'),
    ).toBe('0')
  })

  it('falls back to the first page when the page is not a finite number', () => {
    expect(
      paramsOf(buildAdminPageUrl('/api/admin/users', { page: NaN })).get(
        'page',
      ),
    ).toBe('0')
  })
})
