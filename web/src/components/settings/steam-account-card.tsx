import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { ExternalLink, Link2, Link2Off } from 'lucide-react'
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
import { apiFetch } from '@/services/api'

const linkSchema = z.object({
  steamId: z
    .string()
    .regex(/^\d{17}$/, 'SteamID64 must be exactly 17 digits'),
})

export function SteamAccountCard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const manualLinkForm = useForm({
    defaultValues: { steamId: '' },
    validators: { onSubmit: linkSchema },
    onSubmit: async ({ value }) => {
      const res = await apiFetch('/api/me/steam/link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => null)
        toast.error(data?.message ?? 'Could not link this Steam account.')
        return
      }
      toast.success('Steam account linked.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
    },
  })

  const handleUnlink = async () => {
    const res = await apiFetch('/api/me/steam/unlink', { method: 'DELETE' })
    if (!res.ok) {
      toast.error('Could not unlink Steam account.')
      return
    }
    toast.success('Steam account unlinked.')
    await queryClient.invalidateQueries({
      queryKey: authQueryOptions.queryKey,
    })
  }

  const handleOpenIdLink = () => {
    window.location.href = '/api/auth/steam/openid/start?action=link'
  }

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
          <Button variant="destructive" onClick={handleUnlink}>
            <Link2Off className="mr-2 size-4" />
            Unlink Steam
          </Button>
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
            Or enter your SteamID64 manually:
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
                  <FieldLabel htmlFor="steamId">SteamID64</FieldLabel>
                  <Input
                    id="steamId"
                    type="text"
                    inputMode="numeric"
                    placeholder="76561198000000000"
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
            <p className="text-muted-foreground text-xs">
              Find your SteamID64 at{' '}
              <a
                href="https://steamid.io/"
                target="_blank"
                rel="noopener noreferrer"
                className="text-foreground inline-flex items-center gap-1 underline"
              >
                steamid.io
                <ExternalLink className="size-3" />
              </a>
              .
            </p>
            <manualLinkForm.Subscribe
              selector={(state) => [state.canSubmit, state.isSubmitting]}
              children={([canSubmit, isSubmitting]) => (
                <Button
                  type="submit"
                  variant="outline"
                  disabled={!canSubmit || isSubmitting}
                >
                  {isSubmitting ? 'Linking...' : 'Link by SteamID'}
                </Button>
              )}
            />
          </form>
        </div>
      </CardContent>
    </Card>
  )
}
