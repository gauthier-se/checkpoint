import {
  HeadContent,
  Outlet,
  Scripts,
  createRootRouteWithContext,
} from '@tanstack/react-router'

import { HotkeysProvider } from '@tanstack/react-hotkeys'
import { ThemeProvider } from 'next-themes'
import appCss from '../styles.css?inline'
import type { QueryClient } from '@tanstack/react-query'
import { seo } from '@/lib/seo'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { ErrorBoundary } from '@/components/errors/error-boundary'
import { ErrorPage } from '@/components/errors/error-page'
import { isApiError } from '@/services/api'

export interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  head: () => ({
    meta: [
      {
        charSet: 'utf-8',
      },
      {
        name: 'viewport',
        content: 'width=device-width, initial-scale=1',
      },
      ...seo({
        title: 'Checkpoint — Your gaming journal',
        description:
          'Checkpoint is your gaming journal — track the games you play, rate and review them, build lists, and follow what your friends are playing.',
      }),
    ],
    links: [
      { rel: 'icon', href: '/favicon.ico' },
      {
        rel: 'icon',
        type: 'image/png',
        sizes: '192x192',
        href: '/logo192.png',
      },
      { rel: 'apple-touch-icon', href: '/logo192.png' },
      { rel: 'manifest', href: '/manifest.json' },
    ],
    styles: [
      {
        children: appCss,
      },
    ],
  }),

  shellComponent: RootDocument,
  component: RootComponent,
  errorComponent: ({ error, reset }) => (
    <ErrorPage
      status={isApiError(error) ? error.status : undefined}
      message={isApiError(error) ? error.message : undefined}
      onRetry={reset}
    />
  ),
  notFoundComponent: () => <ErrorPage status={404} />,
})

function RootComponent() {
  return <Outlet />
}

function RootDocument({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <HeadContent />
        <script
          defer
          src="https://analytics.seyzeriat.com/script.js"
          data-website-id="371c0e6e-eb08-4012-a1fd-0817f6a3775b"
        ></script>
      </head>
      <body>
        <HotkeysProvider>
          <ThemeProvider
            attribute="class"
            defaultTheme="dark"
            forcedTheme="dark"
            disableTransitionOnChange
          >
            <TooltipProvider>
              <ErrorBoundary>{children}</ErrorBoundary>
              <Toaster richColors closeButton position="top-right" />
              <Scripts />
            </TooltipProvider>
          </ThemeProvider>
        </HotkeysProvider>
      </body>
    </html>
  )
}
