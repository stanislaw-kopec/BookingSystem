import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// jsdom has no layout engine. Verify calls here; real scrolling is checked in the browser.
HTMLElement.prototype.scrollIntoView = vi.fn()
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  localStorage.clear()
})
