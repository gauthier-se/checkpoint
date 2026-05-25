import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { User } from '@/types/user'
import { OnboardingChecklist } from '@/components/onboarding/onboarding-checklist'
import { OnboardingStoreContext } from '@/components/onboarding/onboarding-store'

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    user: mockUser,
    isLoading: false,
    logout: vi.fn(),
    invalidate: vi.fn(),
  }),
  authQueryOptions: { queryKey: ['auth', 'me'] },
}))

let mockUser: User | null = null

function baseUser(overrides: Partial<User> = {}): User {
  return {
    id: 'u-1',
    username: 'alice',
    email: 'alice@test.com',
    role: 'USER',
    bio: null,
    picture: null,
    isPrivate: false,
    twoFactorEnabled: false,
    steamId: null,
    steamDisplayName: null,
    onboardingCompletedAt: null,
    onboardingSteps: {},
    ...overrides,
  }
}

function renderWithProviders(ui: React.ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const store = {
    isOpen: false,
    step: 'welcome' as const,
    open: vi.fn(),
    close: vi.fn(),
    setStep: vi.fn(),
  }
  return {
    store,
    ...render(
      <QueryClientProvider client={client}>
        <OnboardingStoreContext.Provider value={store}>
          {ui}
        </OnboardingStoreContext.Provider>
      </QueryClientProvider>,
    ),
  }
}

describe('OnboardingChecklist', () => {
  it('renders nothing when onboarding is already completed', () => {
    mockUser = baseUser({ onboardingCompletedAt: '2026-05-24T00:00:00Z' })
    const { container } = renderWithProviders(<OnboardingChecklist />)
    expect(container).toBeEmptyDOMElement()
  })

  it('shows progress as "X of 8 complete" with the right count', () => {
    mockUser = baseUser({
      onboardingSteps: { welcome: true, picture: true, bio: false },
    })
    renderWithProviders(<OnboardingChecklist />)
    expect(screen.getByText('2 of 8 complete')).toBeInTheDocument()
  })

  it('lists all step labels', () => {
    mockUser = baseUser()
    renderWithProviders(<OnboardingChecklist />)
    expect(screen.getByText('Add a profile picture')).toBeInTheDocument()
    expect(screen.getByText('Write a short bio')).toBeInTheDocument()
    expect(screen.getByText('Link your Steam account')).toBeInTheDocument()
    expect(
      screen.getByText('Enable two-factor authentication'),
    ).toBeInTheDocument()
    expect(screen.getByText('Set notification preferences')).toBeInTheDocument()
    expect(screen.getByText('Pick your favorite games')).toBeInTheDocument()
    expect(screen.getByText('Follow a few people')).toBeInTheDocument()
  })

  it('opens the wizard at a given step when clicked', () => {
    mockUser = baseUser({ onboardingSteps: { welcome: true } })
    const { store } = renderWithProviders(<OnboardingChecklist />)
    const resumeBio = screen.getByRole('button', {
      name: /resume write a short bio/i,
    })
    resumeBio.click()
    expect(store.open).toHaveBeenCalledWith('bio')
  })
})
