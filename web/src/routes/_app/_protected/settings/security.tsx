import { createFileRoute } from '@tanstack/react-router'
import { TwoFactorSettings } from '@/components/settings/two-factor-settings'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/settings/security')({
  head: () => ({
    meta: seo({ title: 'Security — Checkpoint' }),
  }),
  component: SettingsSecurityPage,
})

function SettingsSecurityPage() {
  return <TwoFactorSettings />
}
