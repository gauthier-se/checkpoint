/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import { tanstackStart } from '@tanstack/react-start/plugin/vite'
import viteReact from '@vitejs/plugin-react'
import { nitro } from 'nitro/vite'
import { defineConfig, loadEnv } from 'vite'
import viteTsConfigPaths from 'vite-tsconfig-paths'

const config = defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiInternalUrl = env['API_INTERNAL_URL'] || 'http://localhost:8080'
  // Vitest sets mode to "test"; skip the SSR/Nitro plugins there. They wrap
  // React in ways that break component tests (duplicate hook dispatcher).
  const isTest = mode === 'test'

  return {
    plugins: [
      // this is the plugin that enables path aliases
      viteTsConfigPaths({
        projects: ['./tsconfig.json'],
      }),
      tailwindcss(),
      ...(isTest ? [] : [tanstackStart()]),
      viteReact(),
      ...(isTest ? [] : [nitro()]),
    ],
    resolve: {
      dedupe: ['react', 'react-dom'],
    },
    nitro: {
      serverDir: './',
      routeRules: {
        '/api/**': { proxy: `${apiInternalUrl}/api/**` },
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./vitest.setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/routeTree.gen.ts', 'src/**/*.d.ts', 'src/__tests__/**'],
        // Ratchet baseline: thresholds sit just below the measured floor so
        // regressions trip CI while there's room to grow them upward as the
        // suite expands. Initial measurement: lines 4.81, statements 4.81,
        // functions 32.5, branches 41.86 — see TE-255.
        thresholds: {
          lines: 4,
          statements: 4,
          functions: 30,
          branches: 40,
        },
      },
    },
  }
})

export default config
