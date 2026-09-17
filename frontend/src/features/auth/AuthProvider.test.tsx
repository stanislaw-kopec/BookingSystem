import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { beforeEach, expect, it, vi } from 'vitest'
import { apiDownload, apiRequest } from '../../api/apiClient'
import { advanceSession, sessionStorageKey } from '../../api/sessionEvents'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './hooks/useAuth'

const fetchMock = vi.fn<typeof fetch>()
const currentUser = { username: 'anna', roles: ['CLIENT'] }
function response(payload: unknown, status = 200) { return Response.json(payload, { status }) }
function PrivateForm() {
  const [text, setText] = useState('')
  return <label>Prywatne dane<input value={text} onChange={(event) => setText(event.target.value)} /></label>
}
function Screen() {
  const auth = useAuth()
  return <>
    {auth.error && <p role="alert">{auth.error}</p>}
    {auth.isLoading ? <p>Sprawdzanie</p> : auth.user ? <><p>{auth.user.username}</p><PrivateForm /></> : <p>Gość</p>}
    <button onClick={() => void auth.login('anna', 'password').catch(() => {})}>Zaloguj</button>
    <button onClick={() => void auth.logout().catch(() => {})}>Wyloguj</button>
  </>
}
beforeEach(() => {
  advanceSession()
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})
function mount() { render(<AuthProvider><Screen /></AuthProvider>) }

it.each(['json', 'pdf'])('clears private state after a 401 from %s and allows login again', async (kind) => {
  const user = userEvent.setup()
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  mount()
  await user.type(await screen.findByLabelText('Prywatne dane'), 'tajne dane')
  fetchMock.mockResolvedValueOnce(response({ code: 'UNAUTHENTICATED', message: 'Authentication required' }, 401))
  await act(async () => {
    await expect(kind === 'pdf' ? apiDownload('/api/invoice') : apiRequest('/api/private')).rejects.toMatchObject({ status: 401 })
  })
  expect(screen.queryByDisplayValue('tajne dane')).not.toBeInTheDocument()
  expect(screen.getByRole('alert')).toHaveTextContent('Zaloguj się ponownie')
  fetchMock.mockResolvedValueOnce(response({ headerName: 'X-CSRF-TOKEN', token: 'test' }))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(response({ user: currentUser }))
  await user.click(screen.getByRole('button', { name: 'Zaloguj' }))
  expect(await screen.findByLabelText('Prywatne dane')).toHaveValue('')
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})

it('clears private state immediately after a change in another tab and verifies the session', async () => {
  const user = userEvent.setup()
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  mount()
  await user.type(await screen.findByLabelText('Prywatne dane'), 'tajne dane')
  let resolve!: (value: Response) => void
  fetchMock.mockImplementationOnce(() => new Promise((done) => { resolve = done }))
  act(() => window.dispatchEvent(new StorageEvent('storage', { key: sessionStorageKey, newValue: 'change' })))
  expect(screen.queryByLabelText('Prywatne dane')).not.toBeInTheDocument()
  await act(async () => resolve(response({ user: null })))
  expect(screen.getByText('Gość')).toBeInTheDocument()
})

it('does not reuse a form when another tab logs into an account with the same displayed name', async () => {
  const user = userEvent.setup()
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  mount()
  await user.type(await screen.findByLabelText('Prywatne dane'), 'tajne dane')
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  act(() => window.dispatchEvent(new StorageEvent('storage', { key: sessionStorageKey, newValue: 'change' })))
  expect(await screen.findByLabelText('Prywatne dane')).toHaveValue('')
})

it('rechecks the session when the window receives focus', async () => {
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  mount()
  await screen.findByText('anna')
  fetchMock.mockResolvedValueOnce(response({ user: null }))
  act(() => window.dispatchEvent(new Event('focus')))
  expect(await screen.findByText('Gość')).toBeInTheDocument()
  expect(screen.queryByLabelText('Prywatne dane')).not.toBeInTheDocument()
})

it('does not restore an old identity from a delayed response after logout', async () => {
  const user = userEvent.setup()
  fetchMock.mockResolvedValueOnce(response({ user: currentUser }))
  mount()
  await screen.findByText('anna')
  let resolve!: (value: Response) => void
  fetchMock.mockImplementationOnce(() => new Promise((done) => { resolve = done }))
  act(() => window.dispatchEvent(new Event('focus')))
  fetchMock.mockResolvedValueOnce(response({ headerName: 'X-CSRF-TOKEN', token: 'test' }))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
  await user.click(screen.getByRole('button', { name: 'Wyloguj' }))
  await screen.findByText('Gość')
  await act(async () => resolve(response({ user: currentUser })))
  expect(screen.queryByText('anna')).not.toBeInTheDocument()
  expect(localStorage.getItem(sessionStorageKey)).toBeTruthy()
  expect(localStorage.getItem(sessionStorageKey)).not.toContain('anna')
})
