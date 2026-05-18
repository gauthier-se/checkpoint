import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { z } from 'zod'
import type { NotificationPreferences } from '@/types/notification-preferences'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { FieldLabel } from '@/components/ui/field'
import { Switch } from '@/components/ui/switch'
import { updateNotificationPreferences } from '@/queries/notification-preferences'

const preferencesSchema = z.object({
  followEnabled: z.boolean(),
  likeReviewEnabled: z.boolean(),
  likeListEnabled: z.boolean(),
  likeGameEnabled: z.boolean(),
  commentReplyEnabled: z.boolean(),
  levelUpEnabled: z.boolean(),
  badgeUnlockedEnabled: z.boolean(),
})

const toggles = [
  {
    name: 'followEnabled',
    label: 'New followers',
    description: 'When someone starts following you',
  },
  {
    name: 'likeReviewEnabled',
    label: 'Review likes',
    description: 'When someone likes one of your reviews',
  },
  {
    name: 'likeListEnabled',
    label: 'List likes',
    description: 'When someone likes one of your lists',
  },
  {
    name: 'likeGameEnabled',
    label: 'Game likes',
    description: 'When someone likes a game in your library',
  },
  {
    name: 'commentReplyEnabled',
    label: 'Comment replies',
    description: 'When someone replies to one of your comments',
  },
  {
    name: 'levelUpEnabled',
    label: 'Level ups',
    description: 'When you reach a new level',
  },
  {
    name: 'badgeUnlockedEnabled',
    label: 'Badge unlocks',
    description: 'When you earn a new badge',
  },
] as const

interface NotificationPreferencesFormProps {
  preferences: NotificationPreferences
}

export function NotificationPreferencesForm({
  preferences,
}: NotificationPreferencesFormProps) {
  const queryClient = useQueryClient()

  const form = useForm({
    defaultValues: {
      followEnabled: preferences.followEnabled,
      likeReviewEnabled: preferences.likeReviewEnabled,
      likeListEnabled: preferences.likeListEnabled,
      likeGameEnabled: preferences.likeGameEnabled,
      commentReplyEnabled: preferences.commentReplyEnabled,
      levelUpEnabled: preferences.levelUpEnabled,
      badgeUnlockedEnabled: preferences.badgeUnlockedEnabled,
    },
    validators: {
      onSubmit: preferencesSchema,
    },
    onSubmit: async ({ value }) => {
      await updateNotificationPreferences(value)
      await queryClient.invalidateQueries({
        queryKey: ['notification-preferences'],
      })
      toast.success('Notification preferences updated')
    },
  })

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        form.handleSubmit()
      }}
      className="space-y-6"
    >
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Notification preferences</CardTitle>
          <CardDescription>
            Choose which notifications you want to receive
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {toggles.map((toggle) => (
            <form.Field
              key={toggle.name}
              name={toggle.name}
              children={(field) => (
                <div className="flex items-center justify-between gap-4">
                  <div className="space-y-0.5">
                    <FieldLabel htmlFor={toggle.name}>
                      {toggle.label}
                    </FieldLabel>
                    <p className="text-muted-foreground text-sm">
                      {toggle.description}
                    </p>
                  </div>
                  <Switch
                    id={toggle.name}
                    checked={field.state.value}
                    onCheckedChange={(checked) => field.handleChange(checked)}
                  />
                </div>
              )}
            />
          ))}
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <form.Subscribe
          selector={(state) => [state.canSubmit, state.isSubmitting]}
          children={([canSubmit, isSubmitting]) => (
            <Button type="submit" disabled={!canSubmit || isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Save changes'}
            </Button>
          )}
        />
      </div>
    </form>
  )
}
