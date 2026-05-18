import { useHotkeySequences } from '@tanstack/react-hotkeys'
import { useNavigate } from '@tanstack/react-router'
import { useAuth } from './use-auth'
import { useIsDesktop } from './use-is-desktop'

export function useNavigationHotkeys() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isDesktop = useIsDesktop()
  const username = user?.username

  useHotkeySequences(
    [
      {
        sequence: ['G', 'H'],
        callback: () => {
          void navigate({ to: '/' })
        },
      },
      {
        sequence: ['G', 'G'],
        callback: () => {
          void navigate({ to: '/games', search: { page: 1 } })
        },
      },
      {
        sequence: ['G', 'L'],
        callback: () => {
          void navigate({ to: '/lists', search: { page: 1 } })
        },
      },
      {
        sequence: ['G', 'M'],
        callback: () => {
          void navigate({ to: '/members', search: { page: 1 } })
        },
      },
      {
        sequence: ['G', 'B'],
        callback: () => {
          void navigate({ to: '/leaderboard', search: { sortBy: 'xp' } })
        },
      },
      {
        sequence: ['G', 'W'],
        callback: () => {
          void navigate({ to: '/news', search: { page: 1 } })
        },
      },
      {
        sequence: ['G', 'N'],
        callback: () => {
          void navigate({
            to: '/notifications',
            search: { page: 1, filter: 'all' },
          })
        },
        options: { enabled: isDesktop && !!user },
      },
      {
        sequence: ['G', 'P'],
        callback: () => {
          if (!username) return
          void navigate({
            to: '/profile/$username',
            params: { username },
            search: { tab: 'reviews', page: 1 },
          })
        },
        options: { enabled: isDesktop && !!user },
      },
    ],
    { enabled: isDesktop },
  )
}
