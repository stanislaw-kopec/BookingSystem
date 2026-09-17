import { beforeEach, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './apiClient'
import { advanceSession, subscribeToSessionExpiry } from './sessionEvents'

beforeEach(() => advanceSession())
it('does not expire the session when login credentials are rejected', async () => {
  const expired = vi.fn()
  const unsubscribe = subscribeToSessionExpiry(expired)
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce(Response.json({ headerName: 'X-CSRF-TOKEN', token: 'test' }))
    .mockResolvedValueOnce(Response.json({ message: 'Invalid login' }, { status: 401 })))
  try {
    await expect(apiRequest('/api/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 401 })
    expect(expired).not.toHaveBeenCalled()
  } finally { unsubscribe() }
})

it('ignores a 401 from a request started before a session change', async () => {
  const expired = vi.fn()
  const unsubscribe = subscribeToSessionExpiry(expired)
  let resolve!: (value: Response) => void
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>((done) => { resolve = done })))
  const request = apiRequest('/api/private')
  advanceSession()
  resolve(Response.json({}, { status: 401 }))
  try {
    await expect(request).rejects.toMatchObject({ name: 'AbortError' })
    expect(expired).not.toHaveBeenCalled()
  } finally { unsubscribe() }
})

it('maps validation errors to Polish while preserving exact indexed field paths', () => {
  const error = new ApiError(400, 'Invalid', {
    phoneNumber: 'Enter a valid phone number.',
    'repairItems[2].quantity': 'Quantity must be greater than 0.',
    username: 'This username is already taken.',
    unknown: 'Unexpected validation rule',
  })
  expect(error.fieldErrors).toEqual({
    phoneNumber: 'Podaj poprawny numer telefonu (7–30 znaków).',
    'repairItems[2].quantity': 'Ilość musi wynosić co najmniej 0,01.',
    username: 'Ten login jest już zajęty.',
    unknown: 'Sprawdź wartość tego pola.',
  })
})

it('expires the session when the CSRF preflight is rejected', async () => {
  const expired = vi.fn()
  const unsubscribe = subscribeToSessionExpiry(expired)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(Response.json({ code: 'UNAUTHENTICATED' }, { status: 401 })))
  try {
    await expect(apiRequest('/api/private', { method: 'POST' })).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledOnce()
    expect(fetch).toHaveBeenCalledTimes(1)
  } finally { unsubscribe() }
})

it('discards a successful body decoded after the session changed', async () => {
  let resolveBody!: (value: unknown) => void
  const pendingBody = new Promise((resolve) => { resolveBody = resolve })
  const response = Response.json({})
  vi.spyOn(response, 'json').mockReturnValueOnce(pendingBody)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response))
  const request = apiRequest('/api/private')
  await vi.waitFor(() => expect(response.json).toHaveBeenCalledOnce())
  advanceSession()
  resolveBody({ private: 'old data' })
  await expect(request).rejects.toMatchObject({ name: 'AbortError' })
})
