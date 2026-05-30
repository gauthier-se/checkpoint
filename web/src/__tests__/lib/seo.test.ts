import { describe, expect, it } from 'vitest'

import { seo } from '@/lib/seo'

describe('seo', () => {
  it('emits the title plus twitter/og title tags', () => {
    const tags = seo({ title: 'Games — Checkpoint' })

    expect(tags).toContainEqual({ title: 'Games — Checkpoint' })
    expect(tags).toContainEqual({
      name: 'twitter:title',
      content: 'Games — Checkpoint',
    })
    expect(tags).toContainEqual({
      name: 'og:title',
      content: 'Games — Checkpoint',
    })
    expect(tags).toContainEqual({ name: 'og:type', content: 'website' })
  })

  it('omits description tags when no description is provided', () => {
    const tags = seo({ title: 'Games — Checkpoint' })

    expect(tags.some((t) => t.name === 'description')).toBe(false)
    expect(tags.some((t) => t.name === 'og:description')).toBe(false)
  })

  it('adds description tags for description and og/twitter', () => {
    const tags = seo({
      title: 'Checkpoint',
      description: 'Your gaming journal',
    })

    expect(tags).toContainEqual({
      name: 'description',
      content: 'Your gaming journal',
    })
    expect(tags).toContainEqual({
      name: 'twitter:description',
      content: 'Your gaming journal',
    })
    expect(tags).toContainEqual({
      name: 'og:description',
      content: 'Your gaming journal',
    })
  })

  it('adds keyword and image tags only when provided', () => {
    const withImage = seo({
      title: 'Checkpoint',
      keywords: 'games, journal',
      image: 'https://example.com/cover.png',
    })

    expect(withImage).toContainEqual({
      name: 'keywords',
      content: 'games, journal',
    })
    expect(withImage).toContainEqual({
      name: 'twitter:card',
      content: 'summary_large_image',
    })
    expect(withImage).toContainEqual({
      name: 'og:image',
      content: 'https://example.com/cover.png',
    })

    const withoutImage = seo({ title: 'Checkpoint' })
    expect(withoutImage.some((t) => t.name === 'og:image')).toBe(false)
    expect(withoutImage.some((t) => t.name === 'keywords')).toBe(false)
  })
})
