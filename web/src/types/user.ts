export type OnboardingStepKey =
  | 'welcome'
  | 'picture'
  | 'bio'
  | 'steam'
  | 'twofa'
  | 'notifications'
  | 'favorites'
  | 'follow'

export const ONBOARDING_STEP_ORDER: ReadonlyArray<OnboardingStepKey> = [
  'welcome',
  'picture',
  'bio',
  'steam',
  'twofa',
  'notifications',
  'favorites',
  'follow',
]

export interface User {
  id: string
  username: string
  email: string
  role: string
  bio: string | null
  picture: string | null
  isPrivate: boolean
  twoFactorEnabled: boolean
  steamId: string | null
  steamDisplayName: string | null
  /** ISO timestamp set once the wizard is finished or dismissed; null while still onboarding. */
  onboardingCompletedAt: string | null
  /** Sparse map — missing entries mean "not yet seen". */
  onboardingSteps: Partial<Record<OnboardingStepKey, boolean>>
}
