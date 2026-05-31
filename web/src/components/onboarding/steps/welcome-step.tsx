import { AlignLeft, ArrowRight, Library, Users } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { StepFrame } from '../step-frame'
import { Button } from '@/components/ui/button'
import { authQueryOptions } from '@/hooks/use-auth'
import { updateOnboardingStep } from '@/queries/onboarding'

interface WelcomeStepProps {
  onNext: () => void
}

export function WelcomeStep({ onNext }: WelcomeStepProps) {
  const queryClient = useQueryClient()

  const handleStart = () => {
    onNext()
    // Record the click in the background — don't block the UI on it.
    updateOnboardingStep('welcome', true)
      .catch(() => {
        // Auto-mark / next steps can still complete the flow.
      })
      .finally(() => {
        void queryClient.invalidateQueries({
          queryKey: authQueryOptions.queryKey,
        })
      })
  }

  return (
    <StepFrame
      title="Welcome to CheckPoint"
      description="Let's set up your account in a few short steps. You can skip anything and come back later."
      actions={
        <Button onClick={handleStart}>
          Let&apos;s go
          <ArrowRight className="ml-2 size-4" />
        </Button>
      }
    >
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-lg border p-4">
          <Library className="text-primary mb-2 size-5" />
          <p className="font-medium">Track your library</p>
          <p className="text-muted-foreground text-sm">
            Keep tabs on what you&apos;ve played, beaten, or want to try.
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <AlignLeft className="text-primary mb-2 size-5" />
          <p className="font-medium">Share reviews</p>
          <p className="text-muted-foreground text-sm">
            Rate games, write reviews, and curate lists.
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <Users className="text-primary mb-2 size-5" />
          <p className="font-medium">Connect</p>
          <p className="text-muted-foreground text-sm">
            Follow friends and see what they&apos;re playing.
          </p>
        </div>
      </div>
    </StepFrame>
  )
}
