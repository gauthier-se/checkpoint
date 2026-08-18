import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import type {
  AdminNewsImportSettings,
  ImportableNewsSource,
} from '@/types/admin'
import { IMPORTABLE_NEWS_SOURCES } from '@/types/admin'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import {
  adminNewsImportSettingsQueryKey,
  adminNewsImportSettingsQueryOptions,
  importAdminNews,
  invalidateNews,
  updateAdminNewsImportSettings,
} from '@/queries/admin/news'

const SOURCE_LABELS: Record<ImportableNewsSource, string> = {
  STEAM: 'Steam',
  RSS: 'RSS feeds',
}

/**
 * `MANUAL` is deliberately absent: the API rejects it, since manual articles
 * are written in the editor rather than pulled from anywhere.
 */
export function NewsImportPanel() {
  const queryClient = useQueryClient()
  const { data: settings } = useQuery(adminNewsImportSettingsQueryOptions())

  const mutation = useMutation({
    mutationFn: (source: ImportableNewsSource) => importAdminNews(source),
    onSuccess: async (imported, source) => {
      toast.success(
        imported === 0
          ? `Nothing new from ${SOURCE_LABELS[source]}`
          : `Imported ${imported} article${imported === 1 ? '' : 's'} from ${SOURCE_LABELS[source]}`,
      )
      await invalidateNews(queryClient)
    },
  })

  const quotaReached = settings?.remainingToday === 0

  return (
    <Card className="mb-6">
      <CardHeader>
        <CardTitle className="text-base">Import feeds</CardTitle>
        <CardDescription>
          Pulls the latest articles from a source. Imported articles are
          published as they arrive and are not editable here.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <div className="flex flex-wrap gap-2">
          {IMPORTABLE_NEWS_SOURCES.map((source) => (
            <Button
              key={source}
              variant="outline"
              size="sm"
              disabled={mutation.isPending || quotaReached}
              onClick={() => mutation.mutate(source)}
            >
              {mutation.isPending && mutation.variables === source && (
                <Loader2 className="size-4 animate-spin" />
              )}
              Import from {SOURCE_LABELS[source]}
            </Button>
          ))}
        </div>

        {settings && (
          <>
            <Separator />
            <ImportSettingsForm settings={settings} />
          </>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * Two kinds of control, saved two different ways. The pause switches commit on
 * the spot, because stopping an import is the thing an admin reaches for in a
 * hurry. The numbers sit behind a Save button so a half-typed value never
 * reaches the API.
 */
function ImportSettingsForm({
  settings,
}: {
  settings: AdminNewsImportSettings
}) {
  const queryClient = useQueryClient()

  const [maxPerDay, setMaxPerDay] = useState(
    settings.maxArticlesPerDay?.toString() ?? '',
  )
  const [perGame, setPerGame] = useState(settings.steamNewsPerGame.toString())

  // The imported-today counter is refetched after every run, which re-renders
  // this form; re-syncing on the server values keeps the inputs honest without
  // fighting the user, since the fields only change when the numbers do.
  useEffect(() => {
    setMaxPerDay(settings.maxArticlesPerDay?.toString() ?? '')
  }, [settings.maxArticlesPerDay])
  useEffect(() => {
    setPerGame(settings.steamNewsPerGame.toString())
  }, [settings.steamNewsPerGame])

  const mutation = useMutation({
    mutationFn: updateAdminNewsImportSettings,
    onSuccess: (updated) => {
      queryClient.setQueryData(adminNewsImportSettingsQueryKey, updated)
      toast.success('Import settings saved')
    },
    onError: () => toast.error('Could not save the import settings'),
  })

  const unlimited = maxPerDay.trim() === ''

  const saveNumbers = () => {
    const parsedPerGame = Number(perGame)
    if (!Number.isInteger(parsedPerGame) || parsedPerGame < 1) {
      toast.error('Steam articles per game must be a whole number, at least 1')
      return
    }

    const parsedMax = Number(maxPerDay)
    if (!unlimited && (!Number.isInteger(parsedMax) || parsedMax < 0)) {
      toast.error('The daily maximum must be a whole number, at least 0')
      return
    }

    mutation.mutate({
      steamNewsPerGame: parsedPerGame,
      ...(unlimited ? { unlimited: true } : { maxArticlesPerDay: parsedMax }),
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <fieldset className="flex flex-col gap-3">
        <legend className="text-sm font-medium">Scheduled imports</legend>
        <p className="text-muted-foreground text-sm">
          Pausing a source stops its recurring job. The Import buttons above
          still work, so you can top up by hand while a source is paused.
        </p>
        <SourceToggle
          id="steam-enabled"
          label="Steam, every 6 hours"
          checked={settings.steamEnabled}
          disabled={mutation.isPending}
          onChange={(checked) => mutation.mutate({ steamEnabled: checked })}
        />
        <SourceToggle
          id="rss-enabled"
          label="RSS feeds, every hour"
          checked={settings.rssEnabled}
          disabled={mutation.isPending}
          onChange={(checked) => mutation.mutate({ rssEnabled: checked })}
        />
      </fieldset>

      <fieldset className="flex flex-col gap-3">
        <legend className="text-sm font-medium">Limits</legend>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="max-articles-per-day">Max articles per day</Label>
            <Input
              id="max-articles-per-day"
              className="w-40"
              inputMode="numeric"
              placeholder="No limit"
              value={maxPerDay}
              onChange={(event) => setMaxPerDay(event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="steam-news-per-game">Steam articles per game</Label>
            <Input
              id="steam-news-per-game"
              className="w-40"
              inputMode="numeric"
              value={perGame}
              onChange={(event) => setPerGame(event.target.value)}
            />
          </div>
          <Button size="sm" disabled={mutation.isPending} onClick={saveNumbers}>
            {mutation.isPending && <Loader2 className="size-4 animate-spin" />}
            Save limits
          </Button>
        </div>
        <p className="text-muted-foreground text-sm">
          {settings.importedToday} article
          {settings.importedToday === 1 ? '' : 's'} imported today
          {settings.remainingToday === null
            ? ', with no daily limit set.'
            : `, ${settings.remainingToday} left before the daily limit.`}
        </p>
      </fieldset>
    </div>
  )
}

function SourceToggle({
  id,
  label,
  checked,
  disabled,
  onChange,
}: {
  id: string
  label: string
  checked: boolean
  disabled: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <div className="flex items-center gap-3">
      <Switch
        id={id}
        checked={checked}
        disabled={disabled}
        onCheckedChange={onChange}
      />
      <Label htmlFor={id} className="font-normal">
        {label}
      </Label>
    </div>
  )
}
