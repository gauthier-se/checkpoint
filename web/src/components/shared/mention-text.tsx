import { Link } from '@tanstack/react-router'
import { Fragment } from 'react'
import type { ReactNode } from 'react'

/**
 * Matches an `@pseudo` token (2-30 letters, digits, underscores or hyphens) that
 * is not preceded by a word character or another `@`, mirroring the server-side
 * MentionParser so emails and `@@` sequences are not treated as mentions.
 */
const MENTION_REGEX = /(?<![\w@])@([a-zA-Z0-9_-]{2,30})/g

interface MentionTextProps {
  /** Raw user-generated text that may contain `@username` mentions. */
  content: string | null | undefined
}

/**
 * Renders text with `@username` mentions turned into links to the mentioned
 * user's profile. Non-mention text is rendered verbatim, so the parent element
 * controls whitespace handling (e.g. `whitespace-pre-line`).
 */
export function MentionText({ content }: MentionTextProps) {
  if (!content) {
    return null
  }

  const nodes: Array<ReactNode> = []
  let lastIndex = 0
  let key = 0

  for (const match of content.matchAll(MENTION_REGEX)) {
    const start = match.index
    const pseudo = match[1]

    if (start > lastIndex) {
      nodes.push(
        <Fragment key={key++}>{content.slice(lastIndex, start)}</Fragment>,
      )
    }

    nodes.push(
      <Link
        key={key++}
        to="/profile/$username"
        params={{ username: pseudo }}
        className="font-medium text-primary hover:underline"
      >
        @{pseudo}
      </Link>,
    )

    lastIndex = start + match[0].length
  }

  if (lastIndex < content.length) {
    nodes.push(<Fragment key={key++}>{content.slice(lastIndex)}</Fragment>)
  }

  return <>{nodes}</>
}
