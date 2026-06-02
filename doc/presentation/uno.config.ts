import { defineConfig } from 'unocss'

export default defineConfig({
  shortcuts: {
    'cp-text-gradient':
      'bg-gradient-to-r from-[oklch(0.74_0.16_286)] via-[oklch(0.62_0.21_286)] to-[oklch(0.66_0.17_300)] bg-clip-text text-transparent',
    'cp-card':
      'rounded-xl border border-[var(--cp-border)] bg-[var(--cp-card)] p-4',
    'cp-chip':
      'inline-flex items-center gap-1 rounded-full border border-[var(--cp-border)] bg-[var(--cp-card-2)] px-2.5 py-0.5 text-xs',
  },
  theme: {
    colors: {
      cpprimary: 'var(--cp-primary)',
      cpaccent: 'var(--cp-accent)',
      cpborder: 'var(--cp-border)',
      cpcard: 'var(--cp-card)',
      cpmuted: 'var(--cp-foreground-muted)',
    },
  },
})
