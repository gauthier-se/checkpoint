import { TanStackDevtools } from '@tanstack/react-devtools'
import {
  HeadContent,
  Outlet,
  Scripts,
  createRootRouteWithContext,
} from '@tanstack/react-router'
import { TanStackRouterDevtoolsPanel } from '@tanstack/react-router-devtools'

import { HotkeysProvider } from '@tanstack/react-hotkeys'
import { ThemeProvider } from 'next-themes'
import appCss from '../styles.css?inline'
import type { QueryClient } from '@tanstack/react-query'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'

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
      {
        title: 'Checkpoint - Your gaming journal',
      },
    ],
    styles: [
      {
        children: appCss,
      },
    ],
  }),

  shellComponent: RootDocument,
  component: RootComponent,
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
            defaultTheme="system"
            enableSystem
            storageKey="theme"
            disableTransitionOnChange
          >
            <TooltipProvider>
              {children}
              <Toaster richColors closeButton position="top-right" />
              <TanStackDevtools
                config={{
                  position: 'bottom-right',
                }}
                plugins={[
                  {
                    name: 'Tanstack Router',
                    render: <TanStackRouterDevtoolsPanel />,
                  },
                ]}
              />
              <Scripts />
            </TooltipProvider>
          </ThemeProvider>
        </HotkeysProvider>
      </body>
    </html>
  )
}
