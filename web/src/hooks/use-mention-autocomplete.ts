import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'

import type { MemberCard } from '@/types/member'
import { searchMembersQueryOptions } from '@/queries/members'

/** Minimum partial length before suggestions are fetched (matches the members query gate). */
const MIN_QUERY_LENGTH = 2

/**
 * Matches a trailing `@partial` in the text before the caret. The `(?<![\w@])`
 * lookbehind mirrors the server-side MentionParser so emails and `@@` are ignored.
 */
const ACTIVE_MENTION = /(?<![\w@])@([a-zA-Z0-9_-]{0,30})$/

/** The `@token` currently being typed, i.e. the one ending at the caret. */
export interface ActiveMention {
  /** The partial pseudo typed after `@` (without the `@`); may be empty. */
  query: string
  /** Index of the `@` character in the value. */
  start: number
  /** Caret position (end of the partial). */
  end: number
}

/**
 * Detects the mention being typed at the caret, or `null` if none.
 */
export function detectActiveMention(
  value: string,
  caret: number,
): ActiveMention | null {
  const before = value.slice(0, caret)
  const match = ACTIVE_MENTION.exec(before)
  if (!match) {
    return null
  }
  return { query: match[1], start: match.index, end: caret }
}

/**
 * Replaces the active mention's `@partial` with `@pseudo ` and returns the new
 * value plus the caret position just after the inserted text.
 */
export function applyMention(
  value: string,
  mention: ActiveMention,
  pseudo: string,
): { value: string; caret: number } {
  const insertion = `@${pseudo} `
  const nextValue =
    value.slice(0, mention.start) + insertion + value.slice(mention.end)
  return { value: nextValue, caret: mention.start + insertion.length }
}

/**
 * Resolves the active mention at the caret and the matching member suggestions.
 *
 * @param value the current textarea value
 * @param caret the current caret position
 * @returns the active mention (or null) and the suggested members
 */
export function useMentionAutocomplete(value: string, caret: number) {
  const mention = useMemo(
    () => detectActiveMention(value, caret),
    [value, caret],
  )

  const query = mention?.query ?? ''
  const { data } = useQuery(searchMembersQueryOptions(query))

  const suggestions: Array<MemberCard> =
    mention && query.length >= MIN_QUERY_LENGTH ? (data?.content ?? []) : []

  return { mention, suggestions }
}
