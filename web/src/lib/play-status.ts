import type { PlayStatus } from '@/types/interaction'

/**
 * Single source of truth for play-status presentation. The same six statuses are
 * used by the play log, the library (current status per game), and the profile tabs.
 */
export const PLAY_STATUS_LABELS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'Playing',
  PLAYED: 'Played',
  COMPLETED: 'Completed',
  RETIRED: 'Retired',
  SHELVED: 'Shelved',
  ABANDONED: 'Abandoned',
}

export const PLAY_STATUS_COLORS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
  PLAYED: 'bg-teal-500/15 text-teal-400 border-teal-500/20',
  COMPLETED: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
  RETIRED: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
  SHELVED: 'bg-purple-500/15 text-purple-400 border-purple-500/20',
  ABANDONED: 'bg-red-500/15 text-red-400 border-red-500/20',
}

/** Canonical display order for status pickers and tabs. */
export const PLAY_STATUS_ORDER: ReadonlyArray<PlayStatus> = [
  'ARE_PLAYING',
  'PLAYED',
  'COMPLETED',
  'RETIRED',
  'SHELVED',
  'ABANDONED',
]
