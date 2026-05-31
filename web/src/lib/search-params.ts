/**
 * Shared parsers for TanStack Router `validateSearch` functions. They coerce the
 * loosely-typed raw search record into the narrow types each route expects.
 */

/** Returns the value when it is a non-empty string, otherwise undefined. */
export function parseOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined
}

/**
 * Like {@link parseOptionalString} but trims surrounding whitespace and treats
 * a whitespace-only value as absent.
 */
export function parseTrimmedString(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  const trimmed = value.trim()
  return trimmed === '' ? undefined : trimmed
}

/** Parses a finite number from a search value, otherwise undefined. */
export function parseOptionalNumber(value: unknown): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

/**
 * Parses a non-empty string array. Accepts an array (multi-value) or a single
 * string (backward-compatible deep link); returns undefined when empty.
 */
export function parseStringArray(value: unknown): Array<string> | undefined {
  if (Array.isArray(value)) {
    const arr = value.filter(
      (v): v is string => typeof v === 'string' && v.length > 0,
    )
    return arr.length > 0 ? arr : undefined
  }
  if (typeof value === 'string' && value.length > 0) return [value]
  return undefined
}
