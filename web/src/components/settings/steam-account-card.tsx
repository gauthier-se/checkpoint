import { useForm } from '@tanstack/react-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link2, Link2Off, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { apiFetch, isApiError } from '@/services/api'
import { API_PREFIX } from '@/services/api-config'

const API_URL = import.meta.env.VITE_API_URL ?? ''

const linkSchema = z.object({
  steamId: z
    .string()
    .trim()
    .min(1, 'Please enter a SteamID, profile URL, or vanity name')
    .max(256, 'Input is too long'),
})

type SteamSyncSummary = {
  total: number
  imported: number
  skipped: number
  unmatched: number
}

export function SteamAccountCard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const manualLinkForm = useForm({
    defaultValues: { steamId: '' },
    validators: { onSubmit: linkSchema },
    onSubmit: async ({ value }) => {
      try {
        await apiFetch('/api/me/steam/link', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(value),
        })
      } catch (err) {
        toast.error(
          isApiError(err) ? err.message : 'Could not link this Steam account.',
        )
        return
      }
      toast.success('Steam account linked.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
    },
  })

  const handleUnlink = async () => {
    try {
      await apiFetch('/api/me/steam/unlink', { method: 'DELETE' })
    } catch (err) {
      toast.error(
        isApiError(err) ? err.message : 'Could not unlink Steam account.',
      )
      return
    }
    toast.success('Steam account unlinked.')
    await queryClient.invalidateQueries({
      queryKey: authQueryOptions.queryKey,
    })
  }

  const handleOpenIdLink = () => {
    window.location.href = `${API_URL}${API_PREFIX}/auth/steam/openid/start?action=link`
  }

  const syncMutation = useMutation<SteamSyncSummary>({
    meta: { suppressGlobalError: true },
    mutationFn: async () => {
      const res = await apiFetch('/api/me/steam/sync', { method: 'POST' })
      return (await res.json()) as SteamSyncSummary
    },
    onSuccess: async (summary) => {
      toast.success(
        `Imported ${summary.imported} game${summary.imported === 1 ? '' : 's'} from Steam ` +
          `(${summary.skipped} already in library, ${summary.unmatched} not found in IGDB).`,
      )
      await queryClient.invalidateQueries({ queryKey: ['library', 'me'] })
    },
    onError: (err) => {
      toast.error(
        isApiError(err) ? err.message : 'Could not sync Steam library.',
      )
    },
  })

  if (!user) return null

  if (user.steamId) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Link2 className="text-green-500 size-5" />
            <CardTitle>Steam</CardTitle>
          </div>
          <CardDescription>
            Your Steam account is linked. You can now use "Sign in with Steam"
            at login.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-md border p-3 text-sm">
            <div className="font-medium">
              {user.steamDisplayName ?? 'Linked Steam account'}
            </div>
            <div className="text-muted-foreground font-mono text-xs">
              {user.steamId}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
            >
              <RefreshCw
                className={`mr-2 size-4 ${syncMutation.isPending ? 'animate-spin' : ''}`}
              />
              {syncMutation.isPending ? 'Syncing…' : 'Sync Steam library'}
            </Button>
            <Button variant="destructive" onClick={handleUnlink}>
              <Link2Off className="mr-2 size-4" />
              Unlink Steam
            </Button>
          </div>
          <p className="text-muted-foreground text-xs">
            Your Steam profile and game-details visibility must be Public for
            the sync to read your owned games.
          </p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Link2Off className="text-muted-foreground size-5" />
          <CardTitle>Steam</CardTitle>
        </div>
        <CardDescription>
          Link your Steam account to sync your library and sign in with Steam.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div>
          <Button onClick={handleOpenIdLink}>
            <Link2 className="mr-2 size-4" />
            Connect with Steam
          </Button>
          <p className="text-muted-foreground mt-2 text-xs">
            You'll be redirected to Steam to sign in.
          </p>
        </div>

        <div className="border-t pt-4">
          <p className="text-muted-foreground mb-3 text-sm">
            Or enter your Steam info manually:
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              manualLinkForm.handleSubmit()
            }}
            className="space-y-3"
          >
            <manualLinkForm.Field
              name="steamId"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="steamId">
                    SteamID, profile URL, or vanity name
                  </FieldLabel>
                  <Input
                    id="steamId"
                    type="text"
                    placeholder="alice or https://steamcommunity.com/id/alice"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(e) => field.handleChange(e.target.value)}
                  />
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-sm text-destructive">
                      {field.state.meta.errors
                        .map((err) =>
                          typeof err === 'string'
                            ? err
                            : (err as { message?: string }).message,
                        )
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />
            <div className="text-muted-foreground space-y-1 text-xs">
              <p>Accepted formats:</p>
              <ul className="list-disc space-y-0.5 pl-5">
                <li>17-digit SteamID64 (e.g. 76561198000000000)</li>
                <li>
                  Profile URL (steamcommunity.com/profiles/... or
                  steamcommunity.com/id/...)
                </li>
                <li>Vanity name (the part after /id/ in your profile URL)</li>
              </ul>
            </div>
            <manualLinkForm.Subscribe
              selector={(state) => [state.canSubmit, state.isSubmitting]}
              children={([canSubmit, isSubmitting]) => (
                <Button
                  type="submit"
                  variant="outline"
                  disabled={!canSubmit || isSubmitting}
                >
                  {isSubmitting ? 'Linking...' : 'Link Steam account'}
                </Button>
              )}
            />
          </form>
        </div>
      </CardContent>
    </Card>
  )
}
