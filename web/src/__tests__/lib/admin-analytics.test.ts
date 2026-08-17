import { describe, expect, it } from 'vitest'
import { withoutEmptyRanks } from '@/lib/admin-analytics'

describe('withoutEmptyRanks', () => {
  it('drops the accounts the API pads a ranking with', () => {
    // Verified against a running API: topReviewers returns five users even when
    // only two of them have written anything.
    const ranked = withoutEmptyRanks([
      { id: 'a', label: 'alice', value: 6 },
      { id: 'b', label: 'bob', value: 4 },
      { id: 'c', label: 'carol', value: 0 },
      { id: 'd', label: 'adminweb', value: 0 },
    ])

    expect(ranked.map((datum) => datum.label)).toEqual(['alice', 'bob'])
  })

  it('empties the ranking entirely on a fresh database', () => {
    // Every user comes back at zero before anyone reviews anything, which would
    // otherwise plot a chart of zero-length bars.
    expect(
      withoutEmptyRanks([
        { id: 'a', label: 'alice', value: 0 },
        { id: 'b', label: 'bob', value: 0 },
      ]),
    ).toEqual([])
  })

  it('keeps a fully populated ranking in order', () => {
    const data = [
      { id: 'a', label: 'Celeste', value: 4 },
      { id: 'b', label: 'Hades', value: 2 },
    ]

    expect(withoutEmptyRanks(data)).toEqual(data)
  })
})
