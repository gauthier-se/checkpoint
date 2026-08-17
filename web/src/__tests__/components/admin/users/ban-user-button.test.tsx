import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BanUserButton } from '@/components/admin/users/ban-user-button'

const banMock = vi.fn(() => Promise.resolve())
const unbanMock = vi.fn(() => Promise.resolve())
const toastSuccessMock = vi.fn()

let currentUserId = 'admin-1'

vi.mock('@/queries/admin/users', () => ({
  adminUsersQueryKey: ['admin', 'users'],
  banAdminUser: (...args: Array<unknown>) => banMock(...args),
  unbanAdminUser: (...args: Array<unknown>) => unbanMock(...args),
}))

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ user: { id: currentUserId }, isLoading: false }),
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: vi.fn(),
  },
}))

function renderButton(user: {
  id: string
  username: string
  banned: boolean | null
}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <BanUserButton user={user} />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  currentUserId = 'admin-1'
  banMock.mockClear()
  unbanMock.mockClear()
  toastSuccessMock.mockClear()
})

describe('BanUserButton', () => {
  it('bans an active account after confirmation', async () => {
    renderButton({ id: 'user-1', username: 'alpha', banned: false })

    fireEvent.click(screen.getByRole('button', { name: 'Ban' }))
    expect(await screen.findByText('Ban alpha?')).toBeInTheDocument()

    // The trigger and the dialog's confirm button share the label; the last one
    // in the tree is the confirmation inside the dialog.
    fireEvent.click(screen.getAllByRole('button', { name: 'Ban' }).at(-1)!)

    await waitFor(() => expect(banMock).toHaveBeenCalledWith('user-1'))
    expect(unbanMock).not.toHaveBeenCalled()
    await waitFor(() =>
      expect(toastSuccessMock).toHaveBeenCalledWith('alpha was banned'),
    )
  })

  it('does not call the API until the action is confirmed', () => {
    renderButton({ id: 'user-1', username: 'alpha', banned: false })

    fireEvent.click(screen.getByRole('button', { name: 'Ban' }))

    expect(banMock).not.toHaveBeenCalled()
  })

  it('unbans a banned account', async () => {
    renderButton({ id: 'user-2', username: 'bravo', banned: true })

    fireEvent.click(screen.getByRole('button', { name: 'Unban' }))
    expect(await screen.findByText('Unban bravo?')).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: 'Unban' }).at(-1)!)

    await waitFor(() => expect(unbanMock).toHaveBeenCalledWith('user-2'))
    expect(banMock).not.toHaveBeenCalled()
  })

  it('refuses to ban the signed-in admin', () => {
    currentUserId = 'user-1'
    renderButton({ id: 'user-1', username: 'alpha', banned: false })

    const button = screen.getByRole('button', { name: 'Ban' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('title', 'You cannot ban your own account')

    fireEvent.click(button)
    expect(screen.queryByText('Ban alpha?')).not.toBeInTheDocument()
    expect(banMock).not.toHaveBeenCalled()
  })
})
