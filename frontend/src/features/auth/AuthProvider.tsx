import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { errorMessage } from '../../api/apiClient'
import * as authApi from './api/authApi'
import { AuthContext } from './authContext'
import type { CurrentUser, RegistrationInput } from './types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    authApi.getCurrentUser(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setUser(result)
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(cause))
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [])

  async function login(username: string, password: string) {
    await authApi.login(username, password)
    setUser(await authApi.getCurrentUser())
    setError(null)
  }

  async function register(input: RegistrationInput) {
    await authApi.register(input)
    await authApi.login(input.username, input.password)
    setUser(await authApi.getCurrentUser())
    setError(null)
  }

  async function logout() {
    await authApi.logout()
    setUser(null)
    setError(null)
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, error, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
