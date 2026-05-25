import { createContext, useContext } from 'react'
import type { OnboardingStepKey } from '@/types/user'

export type WizardStep = OnboardingStepKey | 'done'

export interface OnboardingStoreValue {
  isOpen: boolean
  step: WizardStep
  open: (step?: WizardStep) => void
  close: () => void
  setStep: (step: WizardStep) => void
}

export const OnboardingStoreContext =
  createContext<OnboardingStoreValue | null>(null)

export function useOnboardingStore(): OnboardingStoreValue {
  const ctx = useContext(OnboardingStoreContext)
  if (!ctx) {
    throw new Error(
      'useOnboardingStore must be used inside <OnboardingStoreProvider>',
    )
  }
  return ctx
}

const SESSION_STEP_KEY = 'onboarding.currentStep'
const SESSION_RETURN_STEP_KEY = 'onboarding.returnToStep'

export function readPersistedStep(): WizardStep | null {
  if (typeof window === 'undefined') return null
  const v = window.sessionStorage.getItem(SESSION_STEP_KEY)
  return v ? (v as WizardStep) : null
}

export function persistStep(step: WizardStep) {
  if (typeof window === 'undefined') return
  window.sessionStorage.setItem(SESSION_STEP_KEY, step)
}

export function clearPersistedStep() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(SESSION_STEP_KEY)
}

export function setReturnToStep(step: WizardStep) {
  if (typeof window === 'undefined') return
  window.sessionStorage.setItem(SESSION_RETURN_STEP_KEY, step)
}

export function consumeReturnToStep(): WizardStep | null {
  if (typeof window === 'undefined') return null
  const v = window.sessionStorage.getItem(SESSION_RETURN_STEP_KEY)
  if (v) window.sessionStorage.removeItem(SESSION_RETURN_STEP_KEY)
  return v ? (v as WizardStep) : null
}
