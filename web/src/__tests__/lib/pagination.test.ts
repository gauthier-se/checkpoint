import { describe, expect, it } from 'vitest'

import { getPageNumbers } from '@/lib/pagination'

describe('getPageNumbers', () => {
  it('returns an empty array when total is 0', () => {
    expect(getPageNumbers(1, 0)).toEqual([])
  })

  it('returns the full sequence when total <= 7', () => {
    expect(getPageNumbers(1, 5)).toEqual([1, 2, 3, 4, 5])
    expect(getPageNumbers(4, 7)).toEqual([1, 2, 3, 4, 5, 6, 7])
  })

  it('places a single ellipsis on the right when current is near the start', () => {
    expect(getPageNumbers(2, 10)).toEqual([1, 2, 3, '...', 10])
  })

  it('places ellipses on both sides when current is in the middle', () => {
    expect(getPageNumbers(5, 10)).toEqual([1, '...', 4, 5, 6, '...', 10])
  })

  it('places a single ellipsis on the left when current is near the end', () => {
    expect(getPageNumbers(9, 10)).toEqual([1, '...', 8, 9, 10])
  })
})
