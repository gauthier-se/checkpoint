import { describe, expect, it } from 'vitest'

import {
  applyMention,
  detectActiveMention,
} from '@/hooks/use-mention-autocomplete'

describe('detectActiveMention', () => {
  it('detects the mention being typed at the caret', () => {
    expect(detectActiveMention('hello @al', 9)).toEqual({
      query: 'al',
      start: 6,
      end: 9,
    })
  })

  it('detects a mention at the very start of the text', () => {
    expect(detectActiveMention('@bob', 4)).toEqual({
      query: 'bob',
      start: 0,
      end: 4,
    })
  })

  it('returns null when there is no mention before the caret', () => {
    expect(detectActiveMention('hello world', 11)).toBeNull()
  })

  it('returns null once the token is broken by a space', () => {
    expect(detectActiveMention('hey @alice and ', 15)).toBeNull()
  })

  it('does not trigger inside an email address', () => {
    expect(detectActiveMention('write to bob@ex', 15)).toBeNull()
  })

  it('only considers the partial up to the caret, not after it', () => {
    expect(detectActiveMention('hi @al more', 5)).toEqual({
      query: 'a',
      start: 3,
      end: 5,
    })
  })

  it('returns null when the partial exceeds the max pseudo length', () => {
    const tooLong = '@' + 'a'.repeat(31)
    expect(detectActiveMention(tooLong, tooLong.length)).toBeNull()
  })
})

describe('applyMention', () => {
  it('replaces the partial with the full pseudo and a trailing space', () => {
    const mention = { query: 'al', start: 6, end: 9 }
    expect(applyMention('hello @al', mention, 'alice')).toEqual({
      value: 'hello @alice ',
      caret: 13,
    })
  })

  it('preserves text that follows the caret', () => {
    const mention = { query: 'al', start: 0, end: 3 }
    expect(applyMention('@alx', mention, 'alice')).toEqual({
      value: '@alice x',
      caret: 7,
    })
  })
})
