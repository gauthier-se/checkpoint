import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { z } from 'zod'
import { Star } from 'lucide-react'
import type { GameDetail } from '@/types/game'
import type { PlayStatus } from '@/types/interaction'
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
import { Textarea } from '@/components/ui/textarea'
import { TagSelector } from '@/components/games/tag-selector'
import { logPlay } from '@/queries/games'
import { submitPlayLogReview } from '@/queries/review'
import { isApiError } from '@/services/api'

interface PlayLogFormProps {
  game: GameDetail
  onSuccess?: () => void
  onCancel?: () => void
}

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

export function PlayLogForm({ game, onSuccess, onCancel }: PlayLogFormProps) {
  const queryClient = useQueryClient()
  const form = useForm({
    defaultValues: {
      platformId: '',
      status: 'COMPLETED' as PlayStatus,
      timePlayed: 0 as number | undefined,
      startDate: '',
      endDate: '',
      ownership: '',
      isReplay: false,
      tagIds: [] as Array<string>,
      score: undefined as number | undefined,
      reviewContent: '',
      haveSpoilers: false,
    },
    validators: {
      // @ts-expect-error Form library schema types are slightly off
      onChange: playLogSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const playLogRes = await logPlay({
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
        })

        if (value.reviewContent && value.reviewContent.trim().length > 0) {
          await submitPlayLogReview(playLogRes.id, {
            content: value.reviewContent,
            haveSpoilers: value.haveSpoilers === true,
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
        onSuccess?.()
        form.reset()
      } catch (err) {
        toast.error(
          isApiError(err) ? err.message : 'Failed to log play session.',
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
      className="space-y-4 py-4"
    >
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
                  .map((e) => (typeof e === 'string' ? e : (e as any).message))
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

      <form.Field
        name="isReplay"
        children={(field) => (
          <div className="flex items-center gap-2 pt-2">
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

      <div className="my-6 border-t pt-4">
        <h3 className="text-md font-medium mb-4">Rating & Review (Optional)</h3>
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
                </div>
              )
            }}
          />

          <form.Field
            name="reviewContent"
            children={(field) => (
              <div className="flex flex-col gap-2">
                <Label htmlFor="reviewContent">Review text</Label>
                <Textarea
                  id="reviewContent"
                  placeholder="What did you think about this playthrough?"
                  className="resize-y min-h-[80px]"
                  value={field.state.value}
                  onChange={(e) => field.handleChange(e.target.value)}
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
          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" type="button" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit || isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Save Log'}
            </Button>
          </div>
        )}
      />
    </form>
  )
}
