const API_URL = import.meta.env.VITE_API_URL ?? ''

/**
 * Resolve a stored picture reference to a renderable URL.
 *
 * The API stores two kinds of values in `picture` fields:
 * - absolute URLs (OAuth/Steam avatars, seeded Unsplash photos, external news
 *   images) — returned as-is.
 * - relative `/uploads/...` paths for user-uploaded files — served by the API
 *   at its own origin (not proxied by the web app), so they must be prefixed
 *   with `API_URL`.
 *
 * Returns `undefined` for missing pictures so it can be passed straight to
 * `<AvatarImage src>`, which then renders the fallback.
 */
export function resolvePictureUrl(
  picture: string | null | undefined,
): string | undefined {
  if (!picture) return undefined
  if (/^https?:\/\//i.test(picture)) return picture
  return `${API_URL}${picture}`
}
