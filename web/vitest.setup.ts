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

// `cleanup()` unmounts the tree but cannot cancel timers a component scheduled:
// `input-otp`, rendered by the login form's TOTP step, keeps one for caret
// handling. The callback then fires after Vitest has torn the jsdom environment
// down for the next file, reaches for `window`, and throws `ReferenceError:
// window is not defined` as an unhandled error. Every test passes and the run
// still exits non-zero, which reads as a failure of whatever file happened to be
// running at the time.
//
// Tracking timers and cancelling the leftovers after each test keeps a leak from
// one test out of the next. Only `setTimeout` is wrapped: a one-shot timer
// outliving the test that scheduled it is always a leak, whereas an interval
// started at module scope may legitimately be meant to keep running.
// The DOM and Node typings of these globals disagree on the id type, `number`
// against `Timeout`, so ids are carried opaquely and handed straight back to the
// native `clearTimeout` that produced them.
const pendingTimeouts = new Set<unknown>()
const nativeSetTimeout = globalThis.setTimeout
const nativeClearTimeout = globalThis.clearTimeout as unknown as (
  id: unknown,
) => void

globalThis.setTimeout = ((
  ...args: Parameters<typeof globalThis.setTimeout>
) => {
  const id = nativeSetTimeout(...args)
  pendingTimeouts.add(id)
  return id
}) as unknown as typeof globalThis.setTimeout

globalThis.clearTimeout = (id: unknown) => {
  pendingTimeouts.delete(id)
  nativeClearTimeout(id)
}

afterEach(() => {
  cleanup()

  for (const id of pendingTimeouts) {
    nativeClearTimeout(id)
  }
  pendingTimeouts.clear()
})
