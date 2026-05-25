import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { TwoFactorSetup } from '@/components/settings/two-factor-setup'
import { updateOnboardingStep } from '@/queries/onboarding'

interface TwoFaStepProps {
  onNext: () => void
}

export function TwoFaStep({ onNext }: TwoFaStepProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [started, setStarted] = useState(false)

  const handleSkip = () => {
    onNext()
    updateOnboardingStep('twofa', false)
      .catch(() => {})
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  if (user?.twoFactorEnabled) {
    return (
      <StepFrame
        title="Two-factor authentication"
        description="Already enabled. You're good to go."
        actions={<Button onClick={onNext}>Continue</Button>}
      >
        <div className="flex items-center gap-2 rounded-md border p-3 text-sm">
          <ShieldCheck className="text-green-500 size-5" />
          <span>2FA is active on your account.</span>
        </div>
      </StepFrame>
    )
  }

  if (started) {
    return (
      <StepFrame
        title="Enable 2FA (recommended)"
        description="Scan the QR code with your authenticator app, then enter the 6-digit code below."
      >
        <TwoFactorSetup compact autoStart onEnabled={onNext} />
      </StepFrame>
    )
  }

  return (
    <StepFrame
      title="Enable 2FA (recommended)"
      description="Add a second layer of security with an authenticator app. Takes about 30 seconds."
      actions={
        <>
          <Button variant="ghost" onClick={handleSkip}>
            I&apos;ll do this later
          </Button>
          <Button onClick={() => setStarted(true)}>
            <ShieldCheck className="mr-2 size-4" />
            Enable 2FA
          </Button>
        </>
      }
    >
      <p className="text-muted-foreground text-sm">
        You&apos;ll be asked for a 6-digit code from your authenticator app at
        every login.
      </p>
    </StepFrame>
  )
}
