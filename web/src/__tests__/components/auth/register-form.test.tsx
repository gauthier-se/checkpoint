import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ComponentProps, ReactNode } from 'react'

// Import the component after the mocks so it picks them up.
import { RegisterForm } from '@/components/auth/register-form'

const navigateMock = vi.fn()
const apiFetchMock = vi.fn()
const toastErrorMock = vi.fn()
const toastSuccessMock = vi.fn()
const invalidateMock = vi.fn()

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigateMock,
  Link: ({
    children,
    to,
    hash,
    ...rest
  }: {
    children: ReactNode
    to?: string
    hash?: string
  } & ComponentProps<'a'>) => (
    <a href={typeof to === 'string' ? to : '#'} {...rest}>
      {children}
    </a>
  ),
}))

vi.mock('@/services/api', () => ({
  apiFetch: (...args: Array<unknown>) => apiFetchMock(...args),
  isApiError: (e: unknown) =>
    typeof e === 'object' && e !== null && '__isApiError' in e,
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    user: null,
    isLoading: false,
    logout: vi.fn(),
    invalidate: invalidateMock,
  }),
}))

vi.mock('sonner', () => ({
  toast: {
    error: (...args: Array<unknown>) => toastErrorMock(...args),
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
  },
}))

describe('RegisterForm', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    apiFetchMock.mockReset()
    toastErrorMock.mockReset()
    toastSuccessMock.mockReset()
    invalidateMock.mockReset()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders the username, email, password, confirm password, and terms inputs plus a submit button', () => {
    render(<RegisterForm />)

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument()
    // The terms checkbox renders as a Radix button with the correct id.
    expect(document.getElementById('acceptTerms')).not.toBeNull()
    expect(
      screen.getByRole('button', { name: /create account/i }),
    ).toBeInTheDocument()
  })

  it('shows a validation error and does not call the API when the email is invalid', async () => {
    render(<RegisterForm />)

    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: 'not-an-email' },
    })

    await waitFor(() => {
      expect(
        screen.getByText(/please enter a valid email address/i),
      ).toBeInTheDocument()
    })
    expect(apiFetchMock).not.toHaveBeenCalled()
  })

  it('renders a "Continue with Steam" OAuth button alongside Google and Twitch', () => {
    render(<RegisterForm />)

    expect(screen.getByRole('button', { name: /^steam$/i })).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /^google$/i }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /^twitch$/i }),
    ).toBeInTheDocument()
  })

  describe('with Steam prefill', () => {
    const prefill = {
      steamId: '76561198000000000',
      steamDisplayName: 'SteamPersona',
      steamAvatarUrl: 'https://cdn/avatar.jpg',
      steamProfileUrl: 'https://steamcommunity.com/id/persona',
    }

    it('shows the Steam banner, prefills the pseudo, and hides confirm password while blank', () => {
      render(<RegisterForm steamToken="signup.jwt" steamPrefill={prefill} />)

      expect(
        screen.getByText(/creating your account from steam/i),
      ).toBeInTheDocument()
      expect(screen.getByText(/steampersona/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/username/i)).toHaveValue('SteamPersona')
      expect(
        screen.getByLabelText(/password \(optional\)/i),
      ).toBeInTheDocument()
      expect(
        screen.queryByLabelText(/confirm password/i),
      ).not.toBeInTheDocument()
    })

    it('reveals the confirm-password field once the user starts typing a password', async () => {
      render(<RegisterForm steamToken="signup.jwt" steamPrefill={prefill} />)

      fireEvent.change(screen.getByLabelText(/password \(optional\)/i), {
        target: { value: 'pw' },
      })

      await waitFor(() => {
        expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument()
      })
    })

    it('posts to /api/auth/register/steam with the token, invalidates auth, then navigates home', async () => {
      apiFetchMock.mockResolvedValue({ ok: true })

      render(<RegisterForm steamToken="signup.jwt" steamPrefill={prefill} />)

      fireEvent.change(screen.getByLabelText(/email/i), {
        target: { value: 'alice@test.com' },
      })
      fireEvent.click(document.getElementById('acceptTerms')!)
      fireEvent.click(screen.getByRole('button', { name: /create account/i }))

      await waitFor(() => {
        expect(apiFetchMock).toHaveBeenCalledWith(
          '/api/auth/register/steam',
          expect.objectContaining({
            method: 'POST',
            body: expect.stringContaining('"token":"signup.jwt"'),
          }),
        )
      })
      await waitFor(() => {
        expect(invalidateMock).toHaveBeenCalled()
      })
      await waitFor(() => {
        expect(navigateMock).toHaveBeenCalledWith({ to: '/' })
      })
    })
  })
})
