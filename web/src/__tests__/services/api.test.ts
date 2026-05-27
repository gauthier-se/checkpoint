import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { apiFetch } from '@/services/api'

describe('apiFetch', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    fetchMock.mockReset()
    fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('always sends credentials so the session cookie is attached', async () => {
    await apiFetch('/api/me')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, init] = fetchMock.mock.calls[0]
    expect(init?.credentials).toBe('include')
  })

  it('forwards init (method, headers, body) while preserving credentials', async () => {
    await apiFetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pseudo: 'alice' }),
    })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/auth/register')
    expect(init?.method).toBe('POST')
    // Headers are normalized into a `Headers` instance before being forwarded.
    expect(new Headers(init?.headers).get('content-type')).toBe(
      'application/json',
    )
    expect(init?.body).toBe(JSON.stringify({ pseudo: 'alice' }))
    expect(init?.credentials).toBe('include')
  })

  it('returns the Response unchanged on success so callers can inspect status/json themselves', async () => {
    const response = new Response('{"ok":true}', { status: 201 })
    fetchMock.mockResolvedValueOnce(response)

    const result = await apiFetch('/api/anything')

    expect(result).toBe(response)
    expect(result.status).toBe(201)
  })
})
