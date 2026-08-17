import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { User } from '@/types/user'

const { navigateMock, useAuthMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  useAuthMock: vi.fn(),
}))

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (options: unknown) => options,
  Outlet: () => <div data-testid="admin-outlet" />,
  Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
    <a href={to}>{children}</a>
  ),
  useNavigate: () => navigateMock,
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: useAuthMock,
  authQueryOptions: { queryKey: ['auth', 'me'] },
}))

const { AdminLayout } = await import('@/routes/_app/_protected/admin/route')

function makeUser(role: string): User {
  return {
    id: 'user-1',
    username: 'alpha',
    email: 'alpha@example.com',
    role,
    bio: null,
    picture: null,
    isPrivate: false,
    twoFactorEnabled: false,
    steamId: null,
    steamDisplayName: null,
    onboardingCompletedAt: null,
    onboardingSteps: {},
  }
}

function mockAuth(user: User | null, isLoading = false) {
  useAuthMock.mockReturnValue({ user, isLoading })
}

beforeEach(() => {
  navigateMock.mockClear()
})

describe('AdminLayout', () => {
  it('renders the panel shell for an admin', () => {
    mockAuth(makeUser('ADMIN'))

    render(<AdminLayout />)

    expect(
      screen.getByRole('heading', { level: 1, name: 'Admin' }),
    ).toBeInTheDocument()
    expect(screen.getByTestId('admin-outlet')).toBeInTheDocument()
    expect(navigateMock).not.toHaveBeenCalled()
  })

  it('links to every admin section', () => {
    mockAuth(makeUser('ADMIN'))

    render(<AdminLayout />)

    const nav = screen.getByRole('navigation', { name: 'Admin sections' })
    expect(nav).toBeInTheDocument()
    for (const label of ['Analytics', 'Users', 'Games', 'Moderation', 'News']) {
      expect(screen.getByRole('link', { name: label })).toBeInTheDocument()
    }
  })

  it('closes the panel when the role was revoked mid-session', () => {
    mockAuth(makeUser('USER'))

    render(<AdminLayout />)

    expect(screen.getByText('Access denied')).toBeInTheDocument()
    expect(screen.queryByTestId('admin-outlet')).not.toBeInTheDocument()
    expect(navigateMock).toHaveBeenCalledWith({ to: '/' })
  })

  it('waits for the session before deciding', () => {
    mockAuth(null, true)

    render(<AdminLayout />)

    expect(screen.getByText('Loading...')).toBeInTheDocument()
    expect(screen.queryByText('Access denied')).not.toBeInTheDocument()
    expect(navigateMock).not.toHaveBeenCalled()
  })
})
