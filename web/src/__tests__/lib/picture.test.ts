import { describe, expect, it } from 'vitest'

import { resolvePictureUrl } from '@/lib/picture'

const API_URL = import.meta.env.VITE_API_URL ?? ''

describe('resolvePictureUrl', () => {
  it('returns undefined for missing pictures', () => {
    expect(resolvePictureUrl(null)).toBeUndefined()
    expect(resolvePictureUrl(undefined)).toBeUndefined()
    expect(resolvePictureUrl('')).toBeUndefined()
  })

  it('returns absolute URLs unchanged', () => {
    const https = 'https://images.unsplash.com/photo-123?w=200'
    const http = 'http://example.com/avatar.png'
    expect(resolvePictureUrl(https)).toBe(https)
    expect(resolvePictureUrl(http)).toBe(http)
  })

  it('prefixes relative upload paths with the API origin', () => {
    expect(resolvePictureUrl('/uploads/profiles/a.jpg')).toBe(
      `${API_URL}/uploads/profiles/a.jpg`,
    )
  })
})
