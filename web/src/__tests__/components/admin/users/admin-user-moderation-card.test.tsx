import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminUserDetail } from '@/types/admin'
import { AdminUserModerationCard } from '@/components/admin/users/admin-user-moderation-card'

const editMock = vi.fn()

vi.mock('@/queries/admin/users', () => ({
  adminUsersQueryKey: ['admin', 'users'],
  editAdminUser: (...args: Array<unknown>) => editMock(...args),
  banAdminUser: vi.fn(),
  unbanAdminUser: vi.fn(),
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ user: { id: 'admin-1' }, isLoading: false }),
}))

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}))

const baseUser: AdminUserDetail = {
  id: 'user-1',
  username: 'alpha',
  email: 'alpha@example.test',
  bio: 'Hello there',
  picture: 'alpha.png',
  isPrivate: false,
  banned: false,
  xpPoint: 120,
  level: 3,
  createdAt: '2026-01-15T10:00:00',
  reviewCount: 4,
  reportCount: 1,
}

function renderCard(user: AdminUserDetail = baseUser) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminUserModerationCard user={user} />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  editMock.mockReset()
  editMock.mockImplementation((_id: string, payload: Record<string, unknown>) =>
    Promise.resolve({ ...baseUser, ...payload }),
  )
})

describe('AdminUserModerationCard', () => {
  it('forces the profile private through the edit endpoint', async () => {
    renderCard()

    fireEvent.click(screen.getByRole('switch', { name: /private profile/i }))

    await waitFor(() =>
      expect(editMock).toHaveBeenCalledWith('user-1', { isPrivate: true }),
    )
  })

  it('clears the bio only after confirmation', async () => {
    renderCard()

    fireEvent.click(screen.getByRole('button', { name: 'Clear bio' }))
    expect(await screen.findByText("Clear alpha's bio?")).toBeInTheDocument()
    expect(editMock).not.toHaveBeenCalled()

    fireEvent.click(
      screen.getAllByRole('button', { name: 'Clear bio' }).at(-1)!,
    )

    await waitFor(() =>
      expect(editMock).toHaveBeenCalledWith('user-1', { clearBio: true }),
    )
  })

  it('clears the picture through its own action', async () => {
    renderCard()

    fireEvent.click(screen.getByRole('button', { name: 'Clear picture' }))
    fireEvent.click(
      screen.getAllByRole('button', { name: 'Clear picture' }).at(-1)!,
    )

    await waitFor(() =>
      expect(editMock).toHaveBeenCalledWith('user-1', { clearPicture: true }),
    )
  })

  it('disables the clear actions when there is nothing to clear', () => {
    renderCard({ ...baseUser, bio: null, picture: null })

    const clearBio = screen.getByRole('button', { name: 'Clear bio' })
    expect(clearBio).toBeDisabled()
    expect(clearBio).toHaveAttribute('title', 'This account has no bio')
    expect(screen.getByRole('button', { name: 'Clear picture' })).toBeDisabled()
  })

  it('does not offer to change a username, an email or a role', () => {
    renderCard()

    expect(screen.queryByLabelText(/username/i)).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/role/i)).not.toBeInTheDocument()
  })
})
