import { describe, expect, it } from 'vitest'
import type { GameDetail } from '@/types/game'
import {
  EMPTY_GAME_FORM,
  describeBlockingReferences,
  parseBlockingReferences,
  toAdminGameFormValues,
  toAdminGamePayload,
} from '@/lib/admin-games'
import { ApiError } from '@/services/api'

describe('parseBlockingReferences', () => {
  it('reads the counts the API returns with a refused deletion', () => {
    const error = new ApiError(409, 'Conflict', 'Referenced', {
      blockingReferences: { library: 3, reviews: 1 },
    })

    expect(parseBlockingReferences(error)).toEqual([
      { label: 'library entries', count: 3 },
      { label: 'review', count: 1 },
    ])
  })

  it('falls back to the raw key for a reference kind it does not know', () => {
    const error = new ApiError(409, 'Conflict', 'Referenced', {
      blockingReferences: { somethingNew: 2 },
    })

    expect(parseBlockingReferences(error)).toEqual([
      { label: 'somethingNew', count: 2 },
    ])
  })

  it('drops zero counts', () => {
    const error = new ApiError(409, 'Conflict', 'Referenced', {
      blockingReferences: { library: 0, likes: 2 },
    })

    expect(parseBlockingReferences(error)).toEqual([
      { label: 'likes', count: 2 },
    ])
  })

  it('returns nothing for an error that is not a reference conflict', () => {
    expect(parseBlockingReferences(new ApiError(500, 'Error', 'Boom'))).toEqual(
      [],
    )
    expect(
      parseBlockingReferences(
        new ApiError(409, 'Conflict', 'Something else entirely'),
      ),
    ).toEqual([])
    expect(parseBlockingReferences(new Error('not an api error'))).toEqual([])
  })
})

describe('describeBlockingReferences', () => {
  it('reads as a sentence fragment', () => {
    expect(
      describeBlockingReferences([
        { label: 'library entries', count: 3 },
        { label: 'review', count: 1 },
      ]),
    ).toBe('3 library entries, 1 review')
  })
})

describe('toAdminGamePayload', () => {
  it('turns blank inputs into null so a cleared field is actually cleared', () => {
    const payload = toAdminGamePayload({
      ...EMPTY_GAME_FORM,
      title: '  Zelda  ',
      description: '   ',
    })

    expect(payload.title).toBe('Zelda')
    expect(payload.description).toBeNull()
    expect(payload.coverUrl).toBeNull()
    expect(payload.releaseDate).toBeNull()
  })

  it('parses the hour fields and leaves unknown ones null', () => {
    const payload = toAdminGamePayload({
      ...EMPTY_GAME_FORM,
      title: 'A Game',
      timeToBeatHastily: '12',
      timeToBeatNormally: '',
      timeToBeatCompletely: 'not a number',
    })

    expect(payload.timeToBeatHastily).toBe(12)
    expect(payload.timeToBeatNormally).toBeNull()
    expect(payload.timeToBeatCompletely).toBeNull()
  })

  it('omits empty id sets rather than sending an empty array', () => {
    const payload = toAdminGamePayload({ ...EMPTY_GAME_FORM, title: 'A Game' })

    expect(payload.genreIds).toBeUndefined()
    expect(payload.platformIds).toBeUndefined()
    expect(payload.companyIds).toBeUndefined()
  })

  it('sends the id sets when the admin picked some', () => {
    const payload = toAdminGamePayload({
      ...EMPTY_GAME_FORM,
      title: 'A Game',
      genreIds: ['genre-1'],
    })

    expect(payload.genreIds).toEqual(['genre-1'])
    expect(payload.platformIds).toBeUndefined()
  })
})

describe('toAdminGameFormValues', () => {
  const game: GameDetail = {
    id: 'game-1',
    title: 'A Game',
    description: null,
    coverUrl: 'cover.png',
    artworkUrl: null,
    trailerYoutubeId: null,
    timeToBeatNormally: 20,
    timeToBeatHastily: null,
    timeToBeatCompletely: null,
    releaseDate: '2026-03-04',
    averageRating: null,
    ratingCount: 0,
    ratingDistribution: [],
    genres: [{ id: 'genre-1', name: 'RPG' }],
    platforms: [{ id: 'platform-1', name: 'PC' }],
    companies: [{ id: 'company-1', name: 'Studio' }],
  }

  it('maps nulls to empty inputs and relations to id arrays', () => {
    const values = toAdminGameFormValues(game)

    expect(values.description).toBe('')
    expect(values.timeToBeatNormally).toBe('20')
    expect(values.timeToBeatHastily).toBe('')
    expect(values.genreIds).toEqual(['genre-1'])
    expect(values.platformIds).toEqual(['platform-1'])
    expect(values.companyIds).toEqual(['company-1'])
  })

  it('trims a timestamped release date down to what a date input accepts', () => {
    const values = toAdminGameFormValues({
      ...game,
      releaseDate: '2026-03-04T00:00:00',
    })

    expect(values.releaseDate).toBe('2026-03-04')
  })

  it('round-trips through the payload without inventing values', () => {
    const payload = toAdminGamePayload(toAdminGameFormValues(game))

    expect(payload.title).toBe('A Game')
    expect(payload.description).toBeNull()
    expect(payload.timeToBeatNormally).toBe(20)
    expect(payload.releaseDate).toBe('2026-03-04')
  })
})
