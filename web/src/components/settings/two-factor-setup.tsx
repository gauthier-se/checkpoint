import { useEffect, useState } from 'react'
import { useForm } from '@tanstack/react-form'
import { useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { TotpInput } from '@/components/auth/totp-input'
import { authQueryOptions } from '@/hooks/use-auth'
import { apiFetch, isApiError } from '@/services/api'

type SetupState = 'idle' | 'pending-qr'

const verifySchema = z.object({
  code: z.string().length(6, 'Code must be exactly 6 digits'),
})

export interface TwoFactorSetupProps {
  /** When true, the component renders without surrounding chrome (used inside the onboarding wizard). */
  compact?: boolean
  /** Called once the user has successfully verified the 6-digit code. */
  onEnabled?: () => void
  /** Skip the internal "Enable 2FA" button and start the setup flow on mount — the parent provides the trigger. */
  autoStart?: boolean
}

/**
 * Portable 2FA enable flow: shows an "Enable 2FA" button → QR code + 6-digit input.
 * Used both on the settings page (wrapped in a Card via {@link TwoFactorSettings})
 * and inline in the onboarding wizard.
 */
export function TwoFactorSetup({
  compact = false,
  onEnabled,
  autoStart = false,
}: TwoFactorSetupProps) {
  const queryClient = useQueryClient()
  const [setupState, setSetupState] = useState<SetupState>('idle')
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string | null>(null)
  const [provisioningUri, setProvisioningUri] = useState<string | null>(null)

  const verifyForm = useForm({
    defaultValues: { code: '' },
    validators: { onSubmit: verifySchema },
    onSubmit: async ({ value }) => {
      try {
        await apiFetch('/api/auth/2fa/verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(value),
        })
      } catch (err) {
        toast.error(
          isApiError(err) ? err.message : 'Invalid code. Please try again.',
        )
        return
      }
      toast.success('Two-factor authentication enabled.')
      setSetupState('idle')
      setQrCodeDataUrl(null)
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
      onEnabled?.()
    },
  })

  const handleSetupStart = async () => {
    let data: { qrCodeDataUrl: string; provisioningUri: string }
    try {
      const res = await apiFetch('/api/auth/2fa/setup', { method: 'POST' })
      data = await res.json()
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Failed to start 2FA setup.')
      return
    }
    setQrCodeDataUrl(data.qrCodeDataUrl)
    setProvisioningUri(data.provisioningUri)
    setSetupState('pending-qr')
  }

  useEffect(() => {
    // Fires once when the parent flips autoStart to true.
    if (autoStart && setupState === 'idle') {
      void handleSetupStart()
    }
  }, [autoStart])

  if (setupState === 'pending-qr' && qrCodeDataUrl) {
    return (
      <div className={compact ? 'space-y-4' : 'space-y-6'}>
        <div className="flex flex-col items-center gap-4">
          <img
            src={qrCodeDataUrl}
            alt="TOTP QR Code"
            className="rounded-md border"
            width={compact ? 160 : 200}
            height={compact ? 160 : 200}
          />
          {provisioningUri && (
            <p className="text-muted-foreground max-w-xs break-all text-center text-xs">
              Can&apos;t scan? Enter this code manually:{' '}
              <span className="font-mono font-semibold">
                {new URL(provisioningUri).searchParams.get('secret')}
              </span>
            </p>
          )}
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            e.stopPropagation()
            verifyForm.handleSubmit()
          }}
          className="space-y-4"
        >
          <verifyForm.Field
            name="code"
            children={(field) => (
              <Field>
                <FieldLabel htmlFor="verify-code">
                  6-digit confirmation code
                </FieldLabel>
                <TotpInput
                  id="verify-code"
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
          <div className="flex gap-2">
            <verifyForm.Subscribe
              selector={(state) => [state.canSubmit, state.isSubmitting]}
              children={([canSubmit, isSubmitting]) => (
                <Button type="submit" disabled={!canSubmit || isSubmitting}>
                  {isSubmitting ? 'Verifying...' : 'Confirm & Enable 2FA'}
                </Button>
              )}
            />
            <Button
              type="button"
              variant="outline"
              onClick={() => setSetupState('idle')}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    )
  }

  if (autoStart) {
    return (
      <p className="text-muted-foreground text-sm">Preparing your QR code...</p>
    )
  }

  return (
    <Button onClick={handleSetupStart}>
      <ShieldCheck className="mr-2 size-4" />
      Enable 2FA
    </Button>
  )
}
