import { useState } from 'react'
import { Outlet, createFileRoute } from '@tanstack/react-router'
import { Footer } from '@/components/layout/footer'
import { Header } from '@/components/layout/header'
import { KeyboardShortcutsDialog } from '@/components/keyboard-shortcuts-dialog'
import { useNotificationsWebSocket } from '@/hooks/use-notifications-websocket'
import { useHelpHotkey } from '@/hooks/use-help-hotkey'
import { authQueryOptions } from '@/hooks/use-auth'

export const Route = createFileRoute('/_app')({
  beforeLoad: async ({ context }) => {
    try {
      await context.queryClient.ensureQueryData(authQueryOptions)
    } catch {
      // SSR auth prefetch failed (typically cross-origin dev where the API
      // auth cookie isn't reachable from the web server). The client will
      // run the query on mount.
    }
  },
  component: AppLayout,
})

function AppLayout() {
  useNotificationsWebSocket()
  const [helpOpen, setHelpOpen] = useState(false)
  const openHelp = () => setHelpOpen(true)

  useHelpHotkey(openHelp)

  return (
    <>
      <Header />
      <Outlet />
      <Footer onOpenKeymaps={openHelp} />
      <KeyboardShortcutsDialog open={helpOpen} onOpenChange={setHelpOpen} />
    </>
  )
}
