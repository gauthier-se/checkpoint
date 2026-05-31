import {
  Bookmark,
  CheckCircle2,
  Flag,
  Pause,
  PlayCircle,
  XCircle,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
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

/** Lucide icon per status, shared by the status tab bar and the game cards. */
export const PLAY_STATUS_ICONS: Record<PlayStatus, LucideIcon> = {
  ARE_PLAYING: PlayCircle,
  PLAYED: Flag,
  COMPLETED: CheckCircle2,
  RETIRED: Pause,
  SHELVED: Bookmark,
  ABANDONED: XCircle,
}

/** Text-only colour class per status, for icons rendered without a badge. */
export const PLAY_STATUS_ICON_COLORS: Record<PlayStatus, string> = {
  ARE_PLAYING: 'text-blue-400',
  PLAYED: 'text-teal-400',
  COMPLETED: 'text-emerald-400',
  RETIRED: 'text-amber-400',
  SHELVED: 'text-purple-400',
  ABANDONED: 'text-red-400',
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
