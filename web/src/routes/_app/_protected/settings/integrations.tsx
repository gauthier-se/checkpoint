import { useEffect } from 'react'
import { createFileRoute, useSearch } from '@tanstack/react-router'
import { toast } from 'sonner'
import { SteamAccountCard } from '@/components/settings/steam-account-card'

import { seo } from '@/lib/seo'

interface IntegrationsSearch {
  linked?: string
}

export const Route = createFileRoute('/_app/_protected/settings/integrations')({
  head: () => ({
    meta: seo({ title: 'Integrations — Checkpoint' }),
  }),
  validateSearch: (search: Record<string, unknown>): IntegrationsSearch => ({
    linked: typeof search.linked === 'string' ? search.linked : undefined,
  }),
  component: SettingsIntegrationsPage,
})

function SettingsIntegrationsPage() {
  const { linked } = useSearch({
    from: '/_app/_protected/settings/integrations',
  })

  useEffect(() => {
    if (linked === 'steam') {
      toast.success('Steam account linked via Steam sign-in.')
    }
  }, [linked])

  return <SteamAccountCard />
}
