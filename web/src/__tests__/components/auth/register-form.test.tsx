import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ComponentProps, ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const navigateMock = vi.fn()
const apiFetchMock = vi.fn()
const toastErrorMock = vi.fn()
const toastSuccessMock = vi.fn()

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => navigateMock,
  Link: ({
    children,
    to,
    hash,
    ...rest
  }: { children: ReactNode; to?: string; hash?: string } & ComponentProps<'a'>) => (
    <a href={typeof to === 'string' ? to : '#'} {...rest}>
      {children}
    </a>
  ),
}))

vi.mock('@/services/api', () => ({
  apiFetch: (...args: Array<unknown>) => apiFetchMock(...args),
}))

vi.mock('sonner', () => ({
  toast: {
    error: (...args: Array<unknown>) => toastErrorMock(...args),
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
  },
}))

// Import the component after the mocks so it picks them up.
import { RegisterForm } from '@/components/auth/register-form'

describe('RegisterForm', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    apiFetchMock.mockReset()
    toastErrorMock.mockReset()
    toastSuccessMock.mockReset()
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
})
