import { describe, expect, it } from 'vitest'

/**
 * Guards the timer cancellation in `vitest.setup.ts`.
 *
 * A timer left pending by one test fires later, after Vitest has torn the jsdom
 * environment down, and throws `ReferenceError: window is not defined` as an
 * unhandled error. Every test still passes, the run exits non-zero, and the blame
 * lands on whichever file happened to be running. That flake cost a skipped web
 * deployment, since the CD pipeline gates on this suite.
 *
 * The two tests below are ordered on purpose: the first leaks, the second checks
 * the leak was cancelled before it ran.
 */
describe('pending timers are cancelled between tests', () => {
  let fired = false

  it('leaves a timer pending', () => {
    setTimeout(() => {
      fired = true
    }, 50)

    expect(fired).toBe(false)
  })

  it('does not let the previous test’s timer fire', async () => {
    // Comfortably past the 50ms above: without the cleanup, this waits long
    // enough for the callback to have run.
    await new Promise((resolve) => setTimeout(resolve, 300))

    expect(fired).toBe(false)
  })
})
