import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, apiFetch, isApiError } from '@/services/api'

describe('apiFetch error handling', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('throws ApiError parsed from the server ErrorResponse on !res.ok', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          status: 404,
          error: 'Not Found',
          message: 'Game not found',
          timestamp: '2026-05-16T10:30:00',
        }),
        { status: 404, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    await expect(apiFetch('/api/games/missing')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      code: 'Not Found',
      message: 'Game not found',
    })
  })

  it('falls back to a generic ApiError when the body is not valid JSON', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response('not json', { status: 500, statusText: 'Server Error' }),
    )

    const error = await apiFetch('/api/anything').catch((e) => e)

    expect(isApiError(error)).toBe(true)
    expect(error.status).toBe(500)
    expect(error.message).toBe('An unexpected error occurred.')
  })

  it('throws ApiError with status 0 when fetch itself rejects (network failure)', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('network down'))

    const error = await apiFetch('/api/anything').catch((e) => e)

    expect(isApiError(error)).toBe(true)
    expect(error.status).toBe(0)
    expect(error.code).toBe('NetworkError')
  })

  it('does not throw on 2xx responses', async () => {
    const response = new Response('{}', { status: 200 })
    fetchMock.mockResolvedValueOnce(response)

    await expect(apiFetch('/api/ok')).resolves.toBe(response)
  })

  it('keeps the parsed error body on details for structured failures', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          status: 409,
          error: 'Conflict',
          message: 'Game cannot be deleted because it is referenced',
          blockingReferences: { library: 3 },
        }),
        { status: 409 },
      ),
    )

    const error = await apiFetch('/api/admin/games/1', {
      method: 'DELETE',
    }).catch((e) => e)

    expect(error.status).toBe(409)
    expect(error.details?.blockingReferences).toEqual({ library: 3 })
  })

  it('leaves details undefined when the response carries no JSON body', async () => {
    fetchMock.mockResolvedValueOnce(new Response('not json', { status: 500 }))

    const error = await apiFetch('/api/anything').catch((e) => e)

    expect(error.details).toBeUndefined()
  })

  it('isApiError recognizes plain branded objects (SSR-safe)', () => {
    const branded = { __isApiError: true, status: 401, code: 'X', message: 'y' }
    expect(isApiError(branded)).toBe(true)
    expect(isApiError(new Error('plain'))).toBe(false)
    expect(isApiError(null)).toBe(false)
    expect(isApiError(new ApiError(500, 'X', 'y'))).toBe(true)
  })
})
