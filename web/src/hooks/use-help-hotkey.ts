import { useHotkey } from '@tanstack/react-hotkeys'
import { useIsDesktop } from './use-is-desktop'

export function useHelpHotkey(onOpen: () => void) {
  const isDesktop = useIsDesktop()

  useHotkey({ key: '?', shift: true }, () => onOpen(), {
    enabled: isDesktop,
    ignoreInputs: true,
  })
}
