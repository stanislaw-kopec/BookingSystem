import { createContext } from 'react'
import type { CurrentUser } from './types'

interface AuthContextValue {
  user: CurrentUser | null
  isLoading: boolean
  error: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
