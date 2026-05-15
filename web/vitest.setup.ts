import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// jsdom doesn't ship a ResizeObserver implementation, but several Radix UI
// primitives (Checkbox, Select, Slider) call it on mount. A no-op stub keeps
// component tests from crashing without affecting layout assertions.
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver
}

afterEach(() => {
  cleanup()
})
