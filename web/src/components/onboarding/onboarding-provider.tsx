import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  OnboardingStoreContext,
  clearPersistedStep,
  consumeReturnToStep,
  persistStep,
  readPersistedStep,
} from './onboarding-store'
import type { WizardStep } from './onboarding-store'
import { useAuth } from '@/hooks/use-auth'

export function OnboardingProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const { user } = useAuth()
  const [isOpen, setIsOpen] = useState(false)
  const [step, setStepState] = useState<WizardStep>('welcome')

  const open = useCallback((next?: WizardStep) => {
    if (next) {
      setStepState(next)
      persistStep(next)
    }
    setIsOpen(true)
  }, [])

  const close = useCallback(() => {
    setIsOpen(false)
  }, [])

  const setStep = useCallback((next: WizardStep) => {
    setStepState(next)
    persistStep(next)
  }, [])

  // Auto-open when an unboarded user first hits a protected route.
  useEffect(() => {
    if (!user) return
    if (user.onboardingCompletedAt) {
      clearPersistedStep()
      return
    }
    const returnTo = consumeReturnToStep()
    if (returnTo) {
      setStepState(returnTo)
      persistStep(returnTo)
      setIsOpen(true)
      return
    }
    if (!isOpen) {
      const persisted = readPersistedStep()
      if (persisted) {
        setStepState(persisted)
      } else {
        setStepState('welcome')
        persistStep('welcome')
      }
      setIsOpen(true)
    }
    // Keyed on the user — opening is a one-shot decision per session.
  }, [user?.id, user?.onboardingCompletedAt, isOpen])

  const value = useMemo(
    () => ({ isOpen, step, open, close, setStep }),
    [isOpen, step, open, close, setStep],
  )

  return (
    <OnboardingStoreContext.Provider value={value}>
      {children}
    </OnboardingStoreContext.Provider>
  )
}
