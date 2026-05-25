import { queryOptions } from '@tanstack/react-query'
import type { OnboardingStepKey } from '@/types/user'
import { apiFetch } from '@/services/api'

export interface OnboardingState {
  completedAt: string | null
  steps: Partial<Record<OnboardingStepKey, boolean>>
}

export const onboardingQueryOptions = queryOptions({
  queryKey: ['onboarding'],
  queryFn: async (): Promise<OnboardingState> => {
    const res = await apiFetch('/api/me/onboarding')
    return res.json()
  },
  staleTime: 60_000,
})

export async function updateOnboardingStep(
  step: OnboardingStepKey,
  done: boolean,
): Promise<OnboardingState> {
  const res = await apiFetch('/api/me/onboarding', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ step, done }),
  })
  return res.json()
}

export async function completeOnboarding(): Promise<OnboardingState> {
  const res = await apiFetch('/api/me/onboarding/complete', {
    method: 'POST',
  })
  return res.json()
}
