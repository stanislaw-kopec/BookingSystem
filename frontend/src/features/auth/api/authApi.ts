import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { CurrentUser, UserRole } from '../types'

function isRole(value: unknown): value is UserRole {
  return value === 'CLIENT' || value === 'MECHANIC' || value === 'ADMIN'
}

function isUser(value: unknown): value is CurrentUser {
  return isRecord(value) && typeof value.username === 'string'
    && Array.isArray(value.roles) && value.roles.every(isRole)
}

export async function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser | null> {
  const payload = await apiRequest('/api/auth/me', { signal })
  if (isRecord(payload) && (payload.user === null || isUser(payload.user))) return payload.user
  throw new ApiError(502, 'Nie udało się odczytać danych zalogowanego użytkownika.')
}

export async function login(username: string, password: string): Promise<void> {
  await apiRequest('/api/auth/login', {
    method: 'POST',
    body: new URLSearchParams({ username, password }),
  })
}

export async function logout(): Promise<void> {
  await apiRequest('/api/auth/logout', { method: 'POST' })
}
