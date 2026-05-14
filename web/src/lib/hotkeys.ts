export type ShortcutItem = {
  keys: ReadonlyArray<string>
  label: string
  authOnly?: boolean
}

export type ShortcutGroup = {
  title: string
  items: ReadonlyArray<ShortcutItem>
}

export const SHORTCUT_GROUPS: ReadonlyArray<ShortcutGroup> = [
  {
    title: 'Navigation',
    items: [
      { keys: ['G', 'H'], label: 'Go to home' },
      { keys: ['G', 'G'], label: 'Go to games' },
      { keys: ['G', 'L'], label: 'Go to lists' },
      { keys: ['G', 'M'], label: 'Go to members' },
      { keys: ['G', 'W'], label: 'Go to news' },
      { keys: ['G', 'N'], label: 'Go to notifications', authOnly: true },
      { keys: ['G', 'P'], label: 'Go to my profile', authOnly: true },
    ],
  },
  {
    title: 'Global',
    items: [
      { keys: ['Mod', 'K'], label: 'Open search' },
      { keys: ['L', 'G'], label: 'Quick log', authOnly: true },
      { keys: ['?'], label: 'Show keyboard shortcuts' },
    ],
  },
  {
    title: 'Game page',
    items: [
      { keys: ['W'], label: 'Toggle wishlist', authOnly: true },
      { keys: ['B'], label: 'Toggle backlog', authOnly: true },
    ],
  },
]
