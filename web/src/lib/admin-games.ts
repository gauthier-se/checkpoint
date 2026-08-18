import type { AdminGamePayload } from '@/types/admin'
import type { GameDetail } from '@/types/game'
import { isApiError } from '@/services/api'

/**
 * Human labels for the reference kinds the API reports on a refused deletion.
 * Both forms are spelled out: naive pluralisation would produce "library
 * entrys".
 */
const REFERENCE_LABELS: Record<string, { one: string; many: string }> = {
  library: { one: 'library entry', many: 'library entries' },
  playLogs: { one: 'play log', many: 'play logs' },
  reviews: { one: 'review', many: 'reviews' },
  backlogs: { one: 'backlog entry', many: 'backlog entries' },
  wishlists: { one: 'wishlist entry', many: 'wishlist entries' },
  favorites: { one: 'favorite', many: 'favorites' },
  ratings: { one: 'rating', many: 'ratings' },
  likes: { one: 'like', many: 'likes' },
  listEntries: { one: 'list entry', many: 'list entries' },
  dlcs: { one: 'DLC', many: 'DLCs' },
}

export interface BlockingReference {
  label: string
  count: number
}

/**
 * Reads the `blockingReferences` map the API returns with a `409` when a game
 * is still referenced by user data. Returns an empty array for any other error,
 * so callers can treat "no references" as "not a reference conflict".
 */
export function parseBlockingReferences(
  error: unknown,
): Array<BlockingReference> {
  if (!isApiError(error) || error.status !== 409) return []

  const references = error.details?.blockingReferences
  if (typeof references !== 'object' || references === null) return []

  return Object.entries(references as Record<string, unknown>)
    .filter(([, count]) => typeof count === 'number' && count > 0)
    .map(([key, count]) => {
      const value = count as number
      // An unknown key is shown verbatim rather than dropped: a new reference
      // kind on the API side should still explain why the delete was refused.
      const labels = REFERENCE_LABELS[key] ?? { one: key, many: key }
      return {
        label: value === 1 ? labels.one : labels.many,
        count: value,
      }
    })
}

/** "3 library entries, 1 review" */
export function describeBlockingReferences(
  references: Array<BlockingReference>,
): string {
  return references
    .map((reference) => `${reference.count} ${reference.label}`)
    .join(', ')
}

export interface AdminGameFormValues {
  title: string
  description: string
  coverUrl: string
  artworkUrl: string
  trailerYoutubeId: string
  releaseDate: string
  timeToBeatHastily: string
  timeToBeatNormally: string
  timeToBeatCompletely: string
  genreIds: Array<string>
  platformIds: Array<string>
  companyIds: Array<string>
}

function textOrNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function hoursOrNull(value: string): number | null {
  const trimmed = value.trim()
  if (trimmed === '') return null
  const parsed = Number(trimmed)
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * Turns the form's all-strings state into the API payload. Empty inputs become
 * `null` rather than empty strings so clearing a field actually clears it, and
 * the id sets are omitted when empty so an untouched form does not wipe the
 * relations an imported game came with.
 */
export function toAdminGamePayload(
  values: AdminGameFormValues,
): AdminGamePayload {
  return {
    title: values.title.trim(),
    description: textOrNull(values.description),
    coverUrl: textOrNull(values.coverUrl),
    artworkUrl: textOrNull(values.artworkUrl),
    trailerYoutubeId: textOrNull(values.trailerYoutubeId),
    releaseDate: textOrNull(values.releaseDate),
    timeToBeatHastily: hoursOrNull(values.timeToBeatHastily),
    timeToBeatNormally: hoursOrNull(values.timeToBeatNormally),
    timeToBeatCompletely: hoursOrNull(values.timeToBeatCompletely),
    genreIds: values.genreIds.length > 0 ? values.genreIds : undefined,
    platformIds: values.platformIds.length > 0 ? values.platformIds : undefined,
    companyIds: values.companyIds.length > 0 ? values.companyIds : undefined,
  }
}

export const EMPTY_GAME_FORM: AdminGameFormValues = {
  title: '',
  description: '',
  coverUrl: '',
  artworkUrl: '',
  trailerYoutubeId: '',
  releaseDate: '',
  timeToBeatHastily: '',
  timeToBeatNormally: '',
  timeToBeatCompletely: '',
  genreIds: [],
  platformIds: [],
  companyIds: [],
}

/** Prefills the edit form from the public game detail payload. */
export function toAdminGameFormValues(game: GameDetail): AdminGameFormValues {
  return {
    title: game.title,
    description: game.description ?? '',
    coverUrl: game.coverUrl,
    artworkUrl: game.artworkUrl ?? '',
    trailerYoutubeId: game.trailerYoutubeId ?? '',
    // The API returns an ISO date; the date input wants `YYYY-MM-DD`.
    releaseDate: game.releaseDate ? game.releaseDate.slice(0, 10) : '',
    timeToBeatHastily: game.timeToBeatHastily?.toString() ?? '',
    timeToBeatNormally: game.timeToBeatNormally?.toString() ?? '',
    timeToBeatCompletely: game.timeToBeatCompletely?.toString() ?? '',
    genreIds: game.genres.map((genre) => genre.id),
    platformIds: game.platforms.map((platform) => platform.id),
    companyIds: game.companies.map((company) => company.id),
  }
}
