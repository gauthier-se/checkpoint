import { describe, expect, it } from 'vitest'
import type { AdminNews } from '@/types/admin'
import {
  EMPTY_NEWS_FORM,
  isImported,
  isPublished,
  toAdminNewsFormValues,
  toAdminNewsPayload,
} from '@/lib/admin-news'

const draft: AdminNews = {
  id: 'news-1',
  title: 'A draft',
  createdAt: '2026-03-04T18:30:00',
  updatedAt: '2026-03-04T18:30:00',
  source: 'MANUAL',
}

describe('isPublished', () => {
  it('treats an absent publication date as a draft', () => {
    // The API omits the field entirely rather than sending null.
    expect(isPublished(draft)).toBe(false)
    expect(isPublished({ publishedAt: undefined })).toBe(false)
  })

  it('treats a present publication date as published', () => {
    expect(isPublished({ publishedAt: '2026-03-05T09:00:00' })).toBe(true)
  })
})

describe('isImported', () => {
  it('only counts manual articles as editable content', () => {
    expect(isImported({ source: 'MANUAL' })).toBe(false)
    expect(isImported({ source: 'STEAM' })).toBe(true)
    expect(isImported({ source: 'RSS' })).toBe(true)
  })
})

describe('toAdminNewsPayload', () => {
  it('trims the title and nulls out blank optional fields', () => {
    const payload = toAdminNewsPayload({
      title: '  Patch notes  ',
      description: '   ',
      picture: '',
    })

    expect(payload).toEqual({
      title: 'Patch notes',
      description: null,
      picture: null,
    })
  })

  it('keeps the values it was given', () => {
    const payload = toAdminNewsPayload({
      title: 'Patch notes',
      description: 'What changed',
      picture: 'cover.png',
    })

    expect(payload).toEqual({
      title: 'Patch notes',
      description: 'What changed',
      picture: 'cover.png',
    })
  })

  it('sends nothing beyond the three fields the endpoint accepts', () => {
    expect(Object.keys(toAdminNewsPayload(EMPTY_NEWS_FORM)).sort()).toEqual([
      'description',
      'picture',
      'title',
    ])
  })
})

describe('toAdminNewsFormValues', () => {
  it('maps absent optional fields to empty inputs', () => {
    expect(toAdminNewsFormValues(draft)).toEqual({
      title: 'A draft',
      description: '',
      picture: '',
    })
  })

  it('round-trips without inventing values', () => {
    const article: AdminNews = {
      ...draft,
      description: 'Body text',
      picture: 'cover.png',
    }

    expect(toAdminNewsPayload(toAdminNewsFormValues(article))).toEqual({
      title: 'A draft',
      description: 'Body text',
      picture: 'cover.png',
    })
  })
})
