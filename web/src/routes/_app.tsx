import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Outlet, createFileRoute, useRouter } from '@tanstack/react-router'
import { Footer } from '@/components/layout/footer'
import { Header } from '@/components/layout/header'
import { KeyboardShortcutsDialog } from '@/components/keyboard-shortcuts-dialog'
import { OnboardingProvider } from '@/components/onboarding/onboarding-provider'
import { OnboardingWizard } from '@/components/onboarding/onboarding-wizard'
import { useNotificationsWebSocket } from '@/hooks/use-notifications-websocket'
import { useHelpHotkey } from '@/hooks/use-help-hotkey'
import { useKonamiHotkey } from '@/hooks/use-konami-hotkey'
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
  const router = useRouter()
  const { data: user } = useQuery(authQueryOptions)

  const queryClient = useQueryClient()

  // Detect SSR auth failure recovery: if the server rendered as anonymous but the
  // client successfully authenticated on mount, we must invalidate the router to
  // refetch loader data (like MemberCard follow states) with the user session.
  const isHydrated = useRef(false)
  useEffect(() => {
    if (!isHydrated.current) {
      isHydrated.current = true
      return
    }
    // If the user object is now present but we didn't have it during SSR/initial hydration,
    // invalidate everything to refresh the data.
    if (user) {
      void queryClient.invalidateQueries()
      void router.invalidate()
    }
  }, [user?.id, router, queryClient])
  const [helpOpen, setHelpOpen] = useState(false)
  const openHelp = () => setHelpOpen(true)

  useHelpHotkey(openHelp)
  useKonamiHotkey()

  return (
    <OnboardingProvider>
      <div className="flex min-h-svh flex-col">
        <Header />
        <div className="flex-1">
          <Outlet />
        </div>
        <Footer onOpenKeymaps={openHelp} />
      </div>
      <KeyboardShortcutsDialog open={helpOpen} onOpenChange={setHelpOpen} />
      <OnboardingWizard />
    </OnboardingProvider>
  )
}
