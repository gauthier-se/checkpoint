import { createFileRoute } from '@tanstack/react-router'
import { AppearanceSettings } from '@/components/settings/appearance-settings'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/settings/appearance')({
  head: () => ({
    meta: seo({ title: 'Appearance — Checkpoint' }),
  }),
  component: SettingsAppearancePage,
})

function SettingsAppearancePage() {
  return <AppearanceSettings />
}
