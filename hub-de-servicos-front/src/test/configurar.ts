import { afterEach, vi } from 'vitest'

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((media: string) => ({
    matches: false,
    media,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })),
})

afterEach(() => {
  document.body.innerHTML = ''
})
