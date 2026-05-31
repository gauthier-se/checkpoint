<h1 align="center">CheckPoint — Web</h1>

<p align="center">
  <strong>The player-facing web application for CheckPoint</strong>
</p>

---

## Overview

The web module is the public application players use to track their game
library, manage backlogs and wishlists, rate and review games, follow friends,
and earn XP and badges. It is built with [TanStack Start](https://tanstack.com/start)
(React 19 + Vite, SSR) and talks to the [CheckPoint API](../api).

## Tech stack

| Technology | Purpose |
|------------|---------|
| [TanStack Start](https://tanstack.com/start) | React meta-framework with SSR |
| [TanStack Router](https://tanstack.com/router) | File-based routing & loaders |
| [TanStack Query](https://tanstack.com/query) | Server-state fetching & caching |
| [React 19](https://react.dev/) | UI library |
| [Vite](https://vite.dev/) | Build tool & dev server |
| [Tailwind CSS](https://tailwindcss.com/) | Styling |
| [shadcn/ui](https://ui.shadcn.com/) | Component primitives |
| [Vitest](https://vitest.dev/) | Testing |

## Prerequisites

- **Node.js 20+**
- **pnpm**
- A running [CheckPoint API](../api) (defaults to `http://localhost:8080`)

## Getting started

```bash
cd web

# Install dependencies
pnpm install

# Start the dev server (http://localhost:3000)
pnpm dev
```

## Available scripts

| Script | Description |
|--------|-------------|
| `pnpm dev` | Start the Vite dev server on port 3000 |
| `pnpm build` | Build for production |
| `pnpm serve` | Preview the production build |
| `pnpm start` | Run the built SSR server (`.output/server/index.mjs`) |
| `pnpm test` | Run the Vitest test suite |
| `pnpm coverage` | Run tests with coverage |
| `pnpm check` | Auto-fix formatting (prettier) and lint (eslint) |
| `pnpm check:ci` | Verify formatting and lint without writing — **runs in CI** |

> CI runs `pnpm check:ci` and `pnpm test`. Run them locally before opening a PR.
> Eslint uses [`@tanstack/eslint-config`](https://tanstack.com/config/latest/docs/eslint).

## Project structure

```
web/src/
├── routes/       # File-based routes (pages, layouts, loaders)
├── components/   # Reusable UI components (incl. shadcn/ui primitives)
├── queries/      # TanStack Query options / query keys
├── services/     # API client and data-access helpers
├── hooks/        # Custom React hooks
├── lib/          # Utilities and shared helpers
├── types/        # Shared TypeScript types
└── __tests__/    # Vitest tests
```

Routes are file-based: add a file under `src/routes/` and TanStack Router
generates the route tree automatically. The root layout lives in
`src/routes/__root.tsx`.

### Adding shadcn/ui components

```bash
pnpx shadcn@latest add button
```

## Related documentation

- [Root README](../README.md) — project overview and full setup
- [Contributing guide](../CONTRIBUTING.md) — conventions and workflow
- [API README](../api/README.md) — backend the web app consumes
