import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { ShieldCheck, ShieldOff } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { TwoFactorSetup } from './two-factor-setup'
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
import { TotpInput } from '@/components/auth/totp-input'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { apiFetch, isApiError } from '@/services/api'

const disableSchema = z.object({
  password: z.string().min(1, 'Password is required'),
  code: z.string().length(6, 'Code must be exactly 6 digits'),
})

export function TwoFactorSettings() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  const disableForm = useForm({
    defaultValues: { password: '', code: '' },
    validators: { onSubmit: disableSchema },
    onSubmit: async ({ value }) => {
      try {
        await apiFetch('/api/auth/2fa/disable', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(value),
        })
      } catch (err) {
        toast.error(isApiError(err) ? err.message : 'Failed to disable 2FA.')
        return
      }
      toast.success('Two-factor authentication disabled.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
    },
  })

  if (!user) return null

  if (user.twoFactorEnabled) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ShieldCheck className="text-green-500 size-5" />
            <CardTitle>Two-Factor Authentication</CardTitle>
          </div>
          <CardDescription>
            Your account is protected with TOTP-based 2FA. You will be asked for
            a code from your authenticator app at every login.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              e.stopPropagation()
              disableForm.handleSubmit()
            }}
            className="space-y-4"
          >
            <disableForm.Field
              name="password"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="disable-password">
                    Current password
                  </FieldLabel>
                  <Input
                    id="disable-password"
                    type="password"
                    autoComplete="current-password"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(e) => field.handleChange(e.target.value)}
                  />
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-sm text-destructive">
                      {field.state.meta.errors
                        .map((e) =>
                          typeof e === 'string' ? e : (e as any).message,
                        )
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />
            <disableForm.Field
              name="code"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="disable-code">
                    Authenticator code
                  </FieldLabel>
                  <TotpInput
                    id="disable-code"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(value) => field.handleChange(value)}
                    invalid={field.state.meta.errors.length > 0}
                  />
                  {field.state.meta.errors.length > 0 && (
                    <p className="text-sm text-destructive">
                      {field.state.meta.errors
                        .map((e) =>
                          typeof e === 'string' ? e : (e as any).message,
                        )
                        .join(', ')}
                    </p>
                  )}
                </Field>
              )}
            />
            <disableForm.Subscribe
              selector={(state) => [state.canSubmit, state.isSubmitting]}
              children={([canSubmit, isSubmitting]) => (
                <Button
                  type="submit"
                  variant="destructive"
                  disabled={!canSubmit || isSubmitting}
                >
                  <ShieldOff className="mr-2 size-4" />
                  {isSubmitting ? 'Disabling...' : 'Disable 2FA'}
                </Button>
              )}
            />
          </form>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <ShieldOff className="text-muted-foreground size-5" />
          <CardTitle>Two-Factor Authentication</CardTitle>
        </div>
        <CardDescription>
          Add an extra layer of security to your account. Once enabled, you will
          need a code from your authenticator app to sign in.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <TwoFactorSetup />
      </CardContent>
    </Card>
  )
}
