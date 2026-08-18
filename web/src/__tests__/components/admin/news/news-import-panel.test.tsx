import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminNewsImportSettings } from '@/types/admin'
import { NewsImportPanel } from '@/components/admin/news/news-import-panel'

const importMock = vi.fn((_source: string) => Promise.resolve(0))
const updateSettingsMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()

const DEFAULT_SETTINGS: AdminNewsImportSettings = {
  steamEnabled: true,
  rssEnabled: true,
  maxArticlesPerDay: 200,
  steamNewsPerGame: 5,
  importedToday: 12,
  remainingToday: 188,
}

let settings: AdminNewsImportSettings = DEFAULT_SETTINGS

vi.mock('@/queries/admin/news', () => ({
  importAdminNews: (source: string) => importMock(source),
  invalidateNews: () => Promise.resolve(),
  adminNewsImportSettingsQueryKey: ['admin', 'news', 'import-settings'],
  adminNewsImportSettingsQueryOptions: () => ({
    queryKey: ['admin', 'news', 'import-settings'],
    queryFn: () => Promise.resolve(settings),
  }),
  updateAdminNewsImportSettings: (payload: unknown) =>
    updateSettingsMock(payload),
}))

vi.mock('sonner', () => ({
  toast: {
    success: (...args: Array<unknown>) => toastSuccessMock(...args),
    error: (...args: Array<unknown>) => toastErrorMock(...args),
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

/** The settings form only mounts once the query resolves. */
async function renderPanelWithSettings() {
  const result = renderPanel()
  await screen.findByLabelText(/Max articles per day/)
  return result
}

beforeEach(() => {
  settings = DEFAULT_SETTINGS
  importMock.mockReset()
  importMock.mockResolvedValue(0)
  updateSettingsMock.mockReset()
  updateSettingsMock.mockImplementation((payload) =>
    Promise.resolve({ ...settings, ...payload }),
  )
  toastSuccessMock.mockClear()
  toastErrorMock.mockClear()
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

describe('NewsImportPanel settings', () => {
  it('pauses a source as soon as its switch is flipped', async () => {
    await renderPanelWithSettings()

    fireEvent.click(
      screen.getByRole('switch', { name: /Steam, every 6 hours/ }),
    )

    await waitFor(() =>
      expect(updateSettingsMock).toHaveBeenCalledWith({ steamEnabled: false }),
    )
  })

  it('sends the two numbers together when the limits are saved', async () => {
    await renderPanelWithSettings()

    fireEvent.change(screen.getByLabelText(/Max articles per day/), {
      target: { value: '50' },
    })
    fireEvent.change(screen.getByLabelText(/Steam articles per game/), {
      target: { value: '3' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Save limits/ }))

    await waitFor(() =>
      expect(updateSettingsMock).toHaveBeenCalledWith({
        steamNewsPerGame: 3,
        maxArticlesPerDay: 50,
      }),
    )
  })

  it('reads an empty ceiling field as no limit at all', async () => {
    await renderPanelWithSettings()

    fireEvent.change(screen.getByLabelText(/Max articles per day/), {
      target: { value: '' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Save limits/ }))

    await waitFor(() =>
      expect(updateSettingsMock).toHaveBeenCalledWith({
        steamNewsPerGame: 5,
        unlimited: true,
      }),
    )
  })

  it('refuses a fractional per-game count without calling the API', async () => {
    await renderPanelWithSettings()

    fireEvent.change(screen.getByLabelText(/Steam articles per game/), {
      target: { value: '2.5' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Save limits/ }))

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalled())
    expect(updateSettingsMock).not.toHaveBeenCalled()
  })

  it('shows today usage and blocks the import buttons once the ceiling is hit', async () => {
    settings = {
      ...DEFAULT_SETTINGS,
      importedToday: 200,
      remainingToday: 0,
    }
    await renderPanelWithSettings()

    expect(
      screen.getByText(/200 articles imported today, 0 left/),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /Import from Steam/ }),
    ).toBeDisabled()
  })

  it('says when no ceiling is set at all', async () => {
    settings = {
      ...DEFAULT_SETTINGS,
      maxArticlesPerDay: null,
      remainingToday: null,
    }
    await renderPanelWithSettings()

    expect(
      screen.getByText(/12 articles imported today, with no daily limit set/),
    ).toBeInTheDocument()
  })
})
