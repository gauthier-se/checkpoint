import type { AdminNews, AdminNewsPayload } from '@/types/admin'

/**
 * An article is a draft until it has a publication date. The API omits
 * `publishedAt` entirely while it is unset (`@JsonInclude(NON_NULL)`), so
 * "absent" and "draft" are the same thing.
 */
export function isPublished(article: Pick<AdminNews, 'publishedAt'>): boolean {
  return article.publishedAt !== undefined
}

/** Imported articles are owned by their feed and are not editable content. */
export function isImported(article: Pick<AdminNews, 'source'>): boolean {
  return article.source !== 'MANUAL'
}

export interface AdminNewsFormValues {
  title: string
  description: string
  picture: string
}

export const EMPTY_NEWS_FORM: AdminNewsFormValues = {
  title: '',
  description: '',
  picture: '',
}

function textOrNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

/** Blank inputs become `null` so clearing a field actually clears it. */
export function toAdminNewsPayload(
  values: AdminNewsFormValues,
): AdminNewsPayload {
  return {
    title: values.title.trim(),
    description: textOrNull(values.description),
    picture: textOrNull(values.picture),
  }
}

export function toAdminNewsFormValues(article: AdminNews): AdminNewsFormValues {
  return {
    title: article.title,
    description: article.description ?? '',
    picture: article.picture ?? '',
  }
}
