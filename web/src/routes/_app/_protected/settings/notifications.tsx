import { createFileRoute } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { NotificationPreferencesForm } from '@/components/settings/notification-preferences-form'
import { notificationPreferencesQueryOptions } from '@/queries/notification-preferences'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/_protected/settings/notifications')(
  {
    head: () => ({
      meta: seo({ title: 'Notification settings — Checkpoint' }),
    }),
    loader: async ({ context }) => {
      await context.queryClient.ensureQueryData(
        notificationPreferencesQueryOptions(),
      )
    },
    component: SettingsNotificationsPage,
  },
)

function SettingsNotificationsPage() {
  const { data: preferences } = useSuspenseQuery(
    notificationPreferencesQueryOptions(),
  )

  return <NotificationPreferencesForm preferences={preferences} />
}
