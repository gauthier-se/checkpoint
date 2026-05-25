import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { authQueryOptions } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'
import {
  notificationPreferencesQueryOptions,
  updateNotificationPreferences,
} from '@/queries/notification-preferences'
import { isApiError } from '@/services/api'

const toggles = [
  { name: 'followEnabled', label: 'New followers' },
  { name: 'likeReviewEnabled', label: 'Likes on your reviews' },
  { name: 'commentReplyEnabled', label: 'Replies to your comments' },
  { name: 'levelUpEnabled', label: 'Level ups' },
  { name: 'badgeUnlockedEnabled', label: 'Badges unlocked' },
] as const

interface NotificationsStepProps {
  onNext: () => void
}

export function NotificationsStep({ onNext }: NotificationsStepProps) {
  const queryClient = useQueryClient()
  const { data: prefs } = useQuery(notificationPreferencesQueryOptions())
  const [overrides, setOverrides] = useState<Record<string, boolean>>({})
  const [isSaving, setIsSaving] = useState(false)

  const isOn = (name: string): boolean => {
    if (name in overrides) return overrides[name]
    if (!prefs) return true
    return (prefs as unknown as Record<string, boolean>)[name] ?? true
  }

  const handleSave = async () => {
    setIsSaving(true)
    try {
      await updateNotificationPreferences(overrides)
      toast.success('Notification preferences saved.')
      await queryClient.invalidateQueries({
        queryKey: ['notification-preferences'],
      })
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onNext()
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Could not save preferences.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('notifications', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  return (
    <StepFrame
      title="Notification preferences"
      description="Pick what you want to hear about. You can fine-tune these later in Settings."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            Skip for now
          </Button>
          <Button onClick={handleSave} disabled={isSaving}>
            {isSaving ? 'Saving...' : 'Save & continue'}
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {toggles.map((t) => (
          <div
            key={t.name}
            className="flex items-center justify-between gap-4 rounded-md border p-3"
          >
            <label htmlFor={t.name} className="text-sm font-medium">
              {t.label}
            </label>
            <Switch
              id={t.name}
              checked={isOn(t.name)}
              onCheckedChange={(checked) =>
                setOverrides((prev) => ({ ...prev, [t.name]: checked }))
              }
            />
          </div>
        ))}
      </div>
    </StepFrame>
  )
}
