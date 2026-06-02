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
  }
}

// jsdom also lacks IntersectionObserver, which framer-motion's `inView` (used by
// `whileInView` animations) calls on mount. A no-op stub keeps those component
// tests from crashing; visibility-triggered effects simply never fire in tests.
if (typeof globalThis.IntersectionObserver === 'undefined') {
  globalThis.IntersectionObserver = class {
    readonly root = null
    readonly rootMargin = ''
    readonly thresholds = []
    observe() {}
    unobserve() {}
    disconnect() {}
    takeRecords() {
      return []
    }
  }
}

afterEach(() => {
  cleanup()
})
