import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { z } from 'zod'
import { Heart, Star } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import type { GamePlayLogRequestDto, PlayStatus } from '@/types/interaction'
import type { PlayLogDetail } from '@/types/play-log'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { MentionTextarea } from '@/components/shared/mention-textarea'
import { TagSelector } from '@/components/games/tag-selector'
import { logPlay } from '@/queries/games'
import { updatePlayLog } from '@/queries/plays'
import {
  deletePlayLogReview,
  submitPlayLogReview,
  updatePlayLogReview,
} from '@/queries/review'
import { isApiError } from '@/services/api'
import { cn } from '@/lib/utils'

interface CreateProps {
  mode?: 'create'
  game: GameDetail
  initialPlayLog?: never
  onSuccess?: () => void
  onCancel?: () => void
  isLiked?: boolean
  onToggleLike?: () => void
  isLikePending?: boolean
}

interface EditProps {
  mode: 'edit'
  game: GameDetail
  initialPlayLog: PlayLogDetail
  onSuccess?: () => void
  onCancel?: () => void
  isLiked?: boolean
  onToggleLike?: () => void
  isLikePending?: boolean
}

type PlayLogFormProps = CreateProps | EditProps

const playLogSchema = z.object({
  platformId: z.string().min(1, 'Platform is required'),
  status: z
    .enum([
      'ARE_PLAYING',
      'PLAYED',
      'COMPLETED',
      'RETIRED',
      'SHELVED',
      'ABANDONED',
    ])
    .optional(),
  timePlayed: z.coerce.number().min(0, 'Must be positive').optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  ownership: z.string().optional(),
  isReplay: z.boolean().optional(),
  score: z.number().min(1).max(10).optional(),
  reviewContent: z.string().optional(),
  haveSpoilers: z.boolean().optional(),
})

export function PlayLogForm(props: PlayLogFormProps) {
  const { game, onSuccess, onCancel } = props
  const isEdit = props.mode === 'edit'
  const initial = isEdit ? props.initialPlayLog : undefined
  const queryClient = useQueryClient()

  const form = useForm({
    defaultValues: {
      platformId: initial?.platformId ?? '',
      status: initial?.status ?? 'COMPLETED',
      timePlayed: initial?.timePlayed ?? 0,
      startDate: initial?.startDate ?? '',
      endDate: initial?.endDate ?? '',
      ownership: initial?.ownership ?? '',
      isReplay: initial?.isReplay ?? false,
      tagIds: initial?.tags.map((t) => t.id) ?? [],
      score: initial?.score ?? undefined,
      reviewContent: initial?.review?.content ?? '',
      haveSpoilers: initial?.review?.haveSpoilers ?? false,
    },
    validators: {
      // @ts-expect-error Form library schema types are slightly off
      onChange: playLogSchema,
    },
    onSubmit: async ({ value }) => {
      const payload: GamePlayLogRequestDto = {
        videoGameId: game.id,
        platformId: value.platformId,
        status: value.status,
        timePlayed: value.timePlayed || undefined,
        startDate: value.startDate || undefined,
        endDate: value.endDate || undefined,
        ownership: value.ownership || undefined,
        isReplay: value.isReplay,
        score: value.score ? value.score : undefined,
        tagIds: value.tagIds.length > 0 ? value.tagIds : undefined,
      }

      try {
        const reviewText = value.reviewContent.trim()
        const haveSpoilers = value.haveSpoilers === true

        if (isEdit && initial) {
          await updatePlayLog(initial.id, payload)

          if (reviewText.length > 0) {
            if (initial.review) {
              await updatePlayLogReview(initial.id, {
                content: reviewText,
                haveSpoilers,
              })
            } else {
              await submitPlayLogReview(initial.id, {
                content: reviewText,
                haveSpoilers,
              })
            }
          } else if (initial.review) {
            await deletePlayLogReview(initial.id)
          }

          void queryClient.invalidateQueries({
            queryKey: ['plays', initial.id],
          })
          void queryClient.invalidateQueries({
            queryKey: ['users', initial.username, 'profile'],
          })
          toast.success('Play session updated.')
        } else {
          const created = await logPlay(payload)
          if (reviewText.length > 0) {
            await submitPlayLogReview(created.id, {
              content: reviewText,
              haveSpoilers,
            })
          }

          // Logging a play moves the game out of wishlist/backlog and into the library,
          // so refresh the user's collection lists and per-game interaction status.
          void queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
          void queryClient.invalidateQueries({ queryKey: ['wishlist', 'me'] })
          void queryClient.invalidateQueries({ queryKey: ['backlog', 'me'] })
          void queryClient.invalidateQueries({
            queryKey: ['games', game.id, 'interaction-status'],
          })

          toast.success('Play session logged successfully!')
          form.reset()
        }

        onSuccess?.()
      } catch (err) {
        toast.error(
          isApiError(err)
            ? err.message
            : isEdit
              ? 'Failed to update play session.'
              : 'Failed to log play session.',
        )
      }
    },
  })

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        void form.handleSubmit()
      }}
      className="space-y-5 pt-2"
    >
      <div className="grid grid-cols-2 gap-4">
        <form.Field
          name="platformId"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>Platform *</Label>
              <Select
                value={field.state.value}
                onValueChange={field.handleChange}
              >
                <SelectTrigger id={field.name}>
                  <SelectValue placeholder="Select platform" />
                </SelectTrigger>
                <SelectContent>
                  {game.platforms.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {field.state.meta.errors.length > 0 ? (
                <p className="text-sm text-destructive">
                  {field.state.meta.errors
                    .map((e) =>
                      typeof e === 'string' ? e : (e as any).message,
                    )
                    .join(', ')}
                </p>
              ) : null}
            </div>
          )}
        />

        <form.Field
          name="status"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>Play Status</Label>
              <Select
                value={field.state.value}
                onValueChange={field.handleChange as any}
              >
                <SelectTrigger id={field.name}>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ARE_PLAYING">Playing</SelectItem>
                  <SelectItem value="PLAYED">Played</SelectItem>
                  <SelectItem value="COMPLETED">Completed</SelectItem>
                  <SelectItem value="RETIRED">Retired</SelectItem>
                  <SelectItem value="SHELVED">Shelved</SelectItem>
                  <SelectItem value="ABANDONED">Abandoned</SelectItem>
                </SelectContent>
              </Select>
            </div>
          )}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <form.Field
          name="startDate"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>Start Date</Label>
              <Input
                type="date"
                id={field.name}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
              />
            </div>
          )}
        />
        <form.Field
          name="endDate"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>End Date</Label>
              <Input
                type="date"
                id={field.name}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
              />
            </div>
          )}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <form.Field
          name="timePlayed"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>Time Played (minutes)</Label>
              <Input
                type="number"
                min="0"
                id={field.name}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.valueAsNumber)}
              />
            </div>
          )}
        />

        <form.Field
          name="ownership"
          children={(field) => (
            <div className="space-y-2">
              <Label htmlFor={field.name}>Ownership</Label>
              <Input
                type="text"
                placeholder="e.g. Owned, Borrowed, Subscription"
                id={field.name}
                value={field.state.value}
                onChange={(e) => field.handleChange(e.target.value)}
              />
            </div>
          )}
        />
      </div>

      <div className="grid grid-cols-2 gap-4 items-end">
        <form.Field
          name="tagIds"
          children={(field) => (
            <div className="space-y-2">
              <Label>Tags</Label>
              <TagSelector
                selectedTagIds={field.state.value}
                onChange={field.handleChange}
              />
            </div>
          )}
        />

        <form.Field
          name="isReplay"
          children={(field) => (
            <div className="flex items-center gap-2 pb-2">
              <Switch
                id={field.name}
                checked={field.state.value}
                onCheckedChange={field.handleChange}
              />
              <Label htmlFor={field.name} className="cursor-pointer">
                This is a replay
              </Label>
            </div>
          )}
        />
      </div>

      <div className="mt-6 border-t pt-6 mb-2">
        <div className="space-y-4">
          <form.Field
            name="score"
            children={(field) => {
              const value = field.state.value ?? 0
              return (
                <div className="flex flex-col gap-2">
                  <Label>
                    Score
                    {value > 0 && (
                      <span className="ml-2 text-xs text-muted-foreground">
                        {(value / 2).toFixed(1)} / 5
                      </span>
                    )}
                  </Label>
                  <div className="flex items-center gap-6">
                    <div className="flex gap-1">
                      {[1, 2, 3, 4, 5].map((star) => {
                        const leftScore = star * 2 - 1
                        const rightScore = star * 2
                        const isFullFilled = value >= rightScore
                        const isHalfFilled = value === leftScore
                        return (
                          <div key={star} className="relative h-7 w-7">
                            <Star
                              aria-hidden
                              className="absolute inset-0 h-7 w-7 text-muted-foreground/30"
                            />
                            {(isFullFilled || isHalfFilled) && (
                              <Star
                                aria-hidden
                                className="absolute inset-0 h-7 w-7 fill-yellow-400 text-yellow-500"
                                style={
                                  isHalfFilled
                                    ? { clipPath: 'inset(0 50% 0 0)' }
                                    : undefined
                                }
                              />
                            )}
                            <button
                              type="button"
                              aria-label={`Set score to ${(leftScore / 2).toFixed(1)}`}
                              className="absolute inset-y-0 left-0 w-1/2 cursor-pointer focus-visible:outline-none"
                              onClick={() => field.handleChange(leftScore)}
                            />
                            <button
                              type="button"
                              aria-label={`Set score to ${(rightScore / 2).toFixed(1)}`}
                              className="absolute inset-y-0 right-0 w-1/2 cursor-pointer focus-visible:outline-none"
                              onClick={() => field.handleChange(rightScore)}
                            />
                          </div>
                        )
                      })}
                    </div>
                    {props.onToggleLike && (
                      <div className="flex items-center gap-4 border-l pl-4">
                        <button
                          type="button"
                          className="relative h-7 w-7 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-sm disabled:opacity-50 disabled:cursor-not-allowed"
                          disabled={props.isLikePending}
                          onClick={props.onToggleLike}
                          title={props.isLiked ? 'Unlike' : 'Like'}
                        >
                          <Heart
                            className={cn(
                              'absolute inset-0 h-7 w-7 transition-colors',
                              props.isLiked
                                ? 'fill-orange-500 text-orange-500'
                                : 'text-muted-foreground/30 hover:text-muted-foreground/50',
                            )}
                          />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )
            }}
          />

          <form.Field
            name="reviewContent"
            children={(field) => (
              <div className="flex flex-col gap-2">
                <Label htmlFor="reviewContent">Review text</Label>
                <MentionTextarea
                  id="reviewContent"
                  placeholder="What did you think about this playthrough?"
                  className="resize-y min-h-[80px]"
                  value={field.state.value}
                  onChange={field.handleChange}
                />
              </div>
            )}
          />

          <form.Field
            name="haveSpoilers"
            children={(field) => (
              <div className="flex items-center gap-2">
                <Switch
                  id="haveSpoilers"
                  checked={field.state.value}
                  onCheckedChange={field.handleChange}
                />
                <Label
                  htmlFor="haveSpoilers"
                  className="font-normal cursor-pointer"
                >
                  Contains spoilers
                </Label>
              </div>
            )}
          />
        </div>
      </div>

      <form.Subscribe
        selector={(s) => [s.canSubmit, s.isSubmitting]}
        children={([canSubmit, isSubmitting]) => (
          <div className="pt-2 flex justify-end gap-2">
            <Button variant="outline" type="button" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit || isSubmitting}>
              {isSubmitting
                ? 'Saving...'
                : isEdit
                  ? 'Save Changes'
                  : 'Save Log'}
            </Button>
          </div>
        )}
      />
    </form>
  )
}
