import { useQueryClient } from '@tanstack/react-query'
import { ArrowRight, CheckCircle2, Circle, X } from 'lucide-react'
import { toast } from 'sonner'
import { useOnboardingStore } from './onboarding-store'
import type { OnboardingStepKey } from '@/types/user'
import { ONBOARDING_STEP_ORDER } from '@/types/user'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { AnimatedCircularProgressBar } from '@/components/magicui/animated-circular-progress-bar'
import { NumberTicker } from '@/components/magicui/number-ticker'
import { authQueryOptions, useAuth } from '@/hooks/use-auth'
import { completeOnboarding } from '@/queries/onboarding'
import { isApiError } from '@/services/api'

const STEP_LABELS: Record<OnboardingStepKey, string> = {
  welcome: 'Welcome',
  picture: 'Add a profile picture',
  bio: 'Write a short bio',
  steam: 'Link your Steam account',
  twofa: 'Enable two-factor authentication',
  notifications: 'Set notification preferences',
  favorites: 'Pick your favorite games',
  follow: 'Follow a few people',
}

export function OnboardingChecklist() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { open } = useOnboardingStore()

  if (!user || user.onboardingCompletedAt) return null

  const steps = user.onboardingSteps
  const total = ONBOARDING_STEP_ORDER.length
  const doneCount = ONBOARDING_STEP_ORDER.filter(
    (k) => steps[k] === true,
  ).length
  const percent = Math.round((doneCount / total) * 100)
  // Each step contributes an equal share of the overall progress.
  const stepWeight = Math.round(100 / total)

  const handleDismiss = async () => {
    try {
      await completeOnboarding()
      toast.success('Dismissed. You can find these tips in Settings later.')
      await queryClient.invalidateQueries({
        queryKey: authQueryOptions.queryKey,
      })
    } catch (err) {
      toast.error(isApiError(err) ? err.message : 'Could not dismiss.')
    }
  }

  return (
    <Card className="mb-8">
      <CardHeader className="flex flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <AnimatedCircularProgressBar
            value={percent}
            gaugePrimaryColor="var(--primary)"
            gaugeSecondaryColor="var(--muted)"
          >
            <span className="text-base font-semibold">
              <NumberTicker value={percent} />%
            </span>
          </AnimatedCircularProgressBar>
          <div>
            <h2 className="text-lg font-semibold">
              Finish setting up CheckPoint
            </h2>
            <p className="text-sm text-muted-foreground">
              {doneCount} of {total} steps complete
            </p>
          </div>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleDismiss}
          aria-label="Dismiss checklist"
        >
          <X className="size-4" />
        </Button>
      </CardHeader>
      <CardContent>
        <ul className="divide-y">
          {ONBOARDING_STEP_ORDER.map((key) => {
            const done = steps[key] === true
            return (
              <li key={key} className="flex items-center justify-between py-2">
                <div className="flex items-center gap-3">
                  {done ? (
                    <CheckCircle2 className="size-5 text-green-500" />
                  ) : (
                    <Circle className="size-5 text-muted-foreground" />
                  )}
                  <span
                    className={done ? 'text-muted-foreground line-through' : ''}
                  >
                    {STEP_LABELS[key]}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={
                      done
                        ? 'text-xs font-medium text-green-500'
                        : 'text-xs font-medium text-muted-foreground'
                    }
                  >
                    {done ? `${stepWeight}%` : `+${stepWeight}%`}
                  </span>
                  {!done && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => open(key)}
                      aria-label={`Resume ${STEP_LABELS[key]}`}
                    >
                      <ArrowRight className="size-4" />
                    </Button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      </CardContent>
    </Card>
  )
}
