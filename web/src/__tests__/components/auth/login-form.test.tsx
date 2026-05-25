import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ComponentProps, ReactNode } from 'react'

// Import the component after the mocks so it picks them up.
import { LoginForm } from '@/components/auth/login-form'

const navigateMock = vi.fn()
const apiFetchMock = vi.fn()
const toastErrorMock = vi.fn()
const invalidateMock = vi.fn()

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigateMock,
  Link: ({
    children,
    to,
    ...rest
  }: {
    children: ReactNode
    to?: string
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
    success: vi.fn(),
  },
}))

describe('LoginForm', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    apiFetchMock.mockReset()
    toastErrorMock.mockReset()
    invalidateMock.mockReset()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders the email/password form and social login buttons by default', () => {
    render(<LoginForm />)

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /continue with steam/i }),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText(/2fa code/i)).not.toBeInTheDocument()
  })

  it('opens directly on the TOTP challenge step when twoFactorRequired is set', () => {
    render(<LoginForm twoFactorRequired />)

    expect(screen.getByLabelText(/2fa code/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /verify/i })).toBeInTheDocument()
    // The credentials step should not be shown.
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument()
  })
})
