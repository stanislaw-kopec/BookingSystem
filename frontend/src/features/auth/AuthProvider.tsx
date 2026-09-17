import { Fragment, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { errorMessage } from '../../api/apiClient'
import { advanceSession, broadcastSessionChange, sessionStorageKey, subscribeToSessionExpiry } from '../../api/sessionEvents'
import * as authApi from './api/authApi'
import { AuthContext } from './authContext'
import type { CurrentUser, RegistrationInput } from './types'

const expiredMessage = 'Sesja wygasła lub została zakończona. Zaloguj się ponownie.'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewRevision, setViewRevision] = useState(0)
  const generation = useRef(0)
  const currentUser = useRef<CurrentUser | null>(null)
  const changingSession = useRef(false)

  function discardViews() {
    currentUser.current = null
    setUser(null)
    setViewRevision((value) => value + 1)
  }

  function applyUser(result: CurrentUser | null) {
    const previous = currentUser.current
    if (previous && (!result || previous.username !== result.username
      || previous.roles.join() !== result.roles.join())) {
      setViewRevision((value) => value + 1)
    }
    currentUser.current = result
    setUser(result)
  }

  useEffect(() => {
    let controller: AbortController | undefined
    async function refresh(clearViews: boolean) {
      if (changingSession.current && !clearViews) return
      controller?.abort()
      controller = new AbortController()
      const signal = controller.signal
      const attempt = ++generation.current
      if (clearViews) {
        advanceSession()
        discardViews()
        setIsLoading(true)
      }
      try {
        const result = await authApi.getCurrentUser(signal)
        if (signal.aborted || attempt !== generation.current) return
        const lostSession = currentUser.current !== null && result === null
        if (lostSession) advanceSession()
        applyUser(result)
        setError(lostSession ? expiredMessage : null)
      } catch (cause) {
        if (signal.aborted || attempt !== generation.current) return
        // Until verification succeeds, private data must not remain on screen.
        advanceSession()
        discardViews()
        setError(errorMessage(cause))
      } finally {
        if (!signal.aborted && attempt === generation.current) setIsLoading(false)
      }
    }
    const unsubscribe = subscribeToSessionExpiry(() => {
      ++generation.current
      controller?.abort()
      discardViews()
      setIsLoading(false)
      setError(expiredMessage)
      broadcastSessionChange()
    })
    function onStorage(event: StorageEvent) {
      if (event.key === sessionStorageKey) void refresh(true)
    }
    function onVisible() {
      if (document.visibilityState === 'visible') void refresh(false)
    }
    function onFocus() { void refresh(false) }
    window.addEventListener('storage', onStorage)
    window.addEventListener('focus', onFocus)
    document.addEventListener('visibilitychange', onVisible)
    void refresh(false)
    return () => {
      controller?.abort()
      unsubscribe()
      window.removeEventListener('storage', onStorage)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [])

  async function authenticate(operation: () => Promise<void>) {
    const attempt = ++generation.current
    advanceSession()
    changingSession.current = true
    try {
      await operation()
      if (attempt !== generation.current) throw new DOMException('Session changed', 'AbortError')
      const result = await authApi.getCurrentUser()
      if (attempt !== generation.current) throw new DOMException('Session changed', 'AbortError')
      applyUser(result)
      setError(null)
      setIsLoading(false)
      broadcastSessionChange()
    } finally {
      changingSession.current = false
    }
  }

  async function login(username: string, password: string) {
    await authenticate(() => authApi.login(username, password))
  }

  async function register(input: RegistrationInput) {
    await authenticate(async () => {
      await authApi.register(input)
      await authApi.login(input.username, input.password)
    })
  }

  async function logout() {
    const attempt = ++generation.current
    advanceSession()
    changingSession.current = true
    try {
      await authApi.logout()
      if (attempt !== generation.current) return
      discardViews()
      setError(null)
      setIsLoading(false)
      broadcastSessionChange()
    } finally {
      changingSession.current = false
    }
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, error, login, register, logout }}>
      <Fragment key={viewRevision}>{children}</Fragment>
    </AuthContext.Provider>
  )
}
