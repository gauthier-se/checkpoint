import type { TopBarDatum } from '@/components/admin/analytics/admin-top-bar-chart'

/**
 * Drops entries with nothing to show.
 *
 * `topReviewers` pads its top five with accounts that have written nothing —
 * on a fresh database every user comes back at `reviewCount: 0`, and even once
 * reviews exist the tail is filled with zeroes. A zero-length bar labelled "0"
 * is noise, so a rank only makes the chart once it has a value.
 */
export function withoutEmptyRanks(
  data: Array<TopBarDatum>,
): Array<TopBarDatum> {
  return data.filter((datum) => datum.value > 0)
}
