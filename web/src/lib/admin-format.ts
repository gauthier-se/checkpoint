/**
 * Formats an API timestamp for the admin tables. The API serialises
 * `LocalDateTime` without a zone, so this renders it in the viewer's locale.
 * Moderation needs the time of day, not just the date: reports arriving minutes
 * apart are common and the order matters when working through a queue.
 */
export function formatAdminDateTime(value: string): string {
  return new Date(value).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
