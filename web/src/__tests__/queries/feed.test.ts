import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { feedQueryOptions } from '@/queries/feed'

type QueryFn = (ctx: unknown) => Promise<unknown>

describe('feedQueryOptions', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ content: [], metadata: {} }), {
        status: 200,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('builds the query key and URL without a type filter', async () => {
    const options = feedQueryOptions(0, 5)

    expect(options.queryKey).toEqual(['feed', 0, 5, null])

    await (options.queryFn as QueryFn)({})

    const [url] = fetchMock.mock.calls[0]
    expect(url.toString().endsWith('/api/v1/me/feed?page=0&size=5')).toBe(true)
  })

  it('includes the type in the query key and URL when provided', async () => {
    const options = feedQueryOptions(2, 20, 'RATING')

    expect(options.queryKey).toEqual(['feed', 2, 20, 'RATING'])

    await (options.queryFn as QueryFn)({})

    const [url] = fetchMock.mock.calls[0]
    expect(url.toString().endsWith('/api/v1/me/feed?page=2&size=20&type=RATING')).toBe(true)
  })
})
