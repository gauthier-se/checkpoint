import { useCallback } from 'react'
import { clearPersistedStep, useOnboardingStore } from './onboarding-store'
import { WelcomeStep } from './steps/welcome-step'
import { PictureStep } from './steps/picture-step'
import { BioStep } from './steps/bio-step'
import { SteamStep } from './steps/steam-step'
import { TwoFaStep } from './steps/twofa-step'
import { NotificationsStep } from './steps/notifications-step'
import { FavoritesStep } from './steps/favorites-step'
import { FollowStep } from './steps/follow-step'
import { DoneStep } from './steps/done-step'
import type { WizardStep } from './onboarding-store'
import { ONBOARDING_STEP_ORDER } from '@/types/user'
import { useAuth } from '@/hooks/use-auth'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog'

const TOTAL_STEPS = ONBOARDING_STEP_ORDER.length

function nextStep(current: WizardStep): WizardStep {
  if (current === 'done') return 'done'
  const idx = ONBOARDING_STEP_ORDER.indexOf(current)
  if (idx === -1 || idx === TOTAL_STEPS - 1) return 'done'
  return ONBOARDING_STEP_ORDER[idx + 1]
}

function progressIndex(step: WizardStep): number {
  if (step === 'done') return TOTAL_STEPS
  return ONBOARDING_STEP_ORDER.indexOf(step) + 1
}

export function OnboardingWizard() {
  const { user } = useAuth()
  const { isOpen, step, close, setStep } = useOnboardingStore()

  const handleNext = useCallback(() => {
    setStep(nextStep(step))
  }, [step, setStep])

  const handleClose = useCallback(() => {
    close()
  }, [close])

  // Only render for users who haven't finished onboarding.
  if (!user || user.onboardingCompletedAt) return null

  const current = progressIndex(step)
  const percent = Math.round((current / TOTAL_STEPS) * 100)

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) handleClose()
      }}
    >
      <DialogContent
        className="sm:max-w-2xl"
        aria-describedby="onboarding-description"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <div className="space-y-2">
          <DialogTitle className="sr-only">Onboarding</DialogTitle>
          <DialogDescription id="onboarding-description" className="sr-only">
            Set up your CheckPoint account
          </DialogDescription>
          <div
            aria-live="polite"
            className="text-muted-foreground text-xs font-medium"
          >
            {step === 'done' ? 'All done' : `Step ${current} of ${TOTAL_STEPS}`}
          </div>
          <div
            className="bg-muted h-1.5 w-full overflow-hidden rounded-full"
            role="progressbar"
            aria-valuenow={percent}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div
              className="bg-primary h-full transition-all"
              style={{ width: `${percent}%` }}
            />
          </div>
        </div>

        <StepBody step={step} onNext={handleNext} onClose={handleClose} />

        {step !== 'done' && (
          <div className="text-center">
            <button
              type="button"
              onClick={handleClose}
              className="text-muted-foreground hover:text-foreground text-xs underline"
            >
              I&apos;ll do this later
            </button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

interface StepBodyProps {
  step: WizardStep
  onNext: () => void
  onClose: () => void
}

function StepBody({ step, onNext, onClose }: StepBodyProps) {
  switch (step) {
    case 'welcome':
      return <WelcomeStep onNext={onNext} />
    case 'picture':
      return <PictureStep onNext={onNext} />
    case 'bio':
      return <BioStep onNext={onNext} />
    case 'steam':
      return <SteamStep onNext={onNext} />
    case 'twofa':
      return <TwoFaStep onNext={onNext} />
    case 'notifications':
      return <NotificationsStep onNext={onNext} />
    case 'favorites':
      return <FavoritesStep onNext={onNext} />
    case 'follow':
      return <FollowStep onNext={onNext} />
    case 'done':
      return (
        <DoneStep
          onClose={() => {
            clearPersistedStep()
            onClose()
          }}
        />
      )
  }
}
