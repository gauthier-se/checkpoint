import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NewsImportPanel } from '@/components/admin/news/news-import-panel'

const importMock = vi.fn((_source: string) => Promise.resolve(0))
const toastSuccessMock = vi.fn()

vi.mock('@/queries/admin/news', () => ({
  importAdminNews: (source: string) => importMock(source),
  invalidateNews: () => Promise.resolve(),
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: vi.fn(),
  },
}))

function renderPanel() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <NewsImportPanel />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  importMock.mockReset()
  importMock.mockResolvedValue(0)
  toastSuccessMock.mockClear()
})

describe('NewsImportPanel', () => {
  it('does not offer MANUAL, which the API rejects', () => {
    renderPanel()

    expect(screen.getByRole('button', { name: /Steam/ })).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /RSS feeds/ }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Manual/i }),
    ).not.toBeInTheDocument()
  })

  it('reports how many articles a run brought in', async () => {
    importMock.mockResolvedValue(3)
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /RSS feeds/ }))

    await waitFor(() => expect(importMock).toHaveBeenCalledWith('RSS'))
    await waitFor(() =>
      expect(toastSuccessMock).toHaveBeenCalledWith(
        'Imported 3 articles from RSS feeds',
      ),
    )
  })

  it('singularises a one-article run', async () => {
    importMock.mockResolvedValue(1)
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Steam/ }))

    await waitFor(() =>
      expect(toastSuccessMock).toHaveBeenCalledWith(
        'Imported 1 article from Steam',
      ),
    )
  })

  it('says so plainly when a run found nothing', async () => {
    renderPanel()

    fireEvent.click(screen.getByRole('button', { name: /Steam/ }))

    await waitFor(() =>
      expect(toastSuccessMock).toHaveBeenCalledWith('Nothing new from Steam'),
    )
  })
})
