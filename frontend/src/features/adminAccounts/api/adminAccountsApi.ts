import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type {
  AdminAccount,
  AdminAccountFilters,
  AdminAccountPage,
  AdminAccountUpdateInput,
  AdminPasswordResetInput,
  CreatableStaffRole,
  ManagedAccountInput,
} from '../types'

function isAdminAccount(value: unknown): value is AdminAccount {
  return isRecord(value)
    && typeof value.id === 'number'
    && Number.isSafeInteger(value.id)
    && typeof value.username === 'string'
    && typeof value.email === 'string'
    && (value.role === 'CLIENT' || value.role === 'MECHANIC' || value.role === 'ADMIN')
    && typeof value.enabled === 'boolean'
}

function adminAccountFrom(value: unknown): AdminAccount {
  if (isAdminAccount(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane konta.')
}

function adminAccountPageFrom(value: unknown): AdminAccountPage {
  if (isRecord(value)
      && Array.isArray(value.content)
      && value.content.every(isAdminAccount)
      && typeof value.page === 'number' && Number.isSafeInteger(value.page)
      && typeof value.size === 'number' && Number.isSafeInteger(value.size)
      && typeof value.totalElements === 'number' && Number.isSafeInteger(value.totalElements)
      && typeof value.totalPages === 'number' && Number.isSafeInteger(value.totalPages)) {
    return {
      content: value.content,
      page: value.page,
      size: value.size,
      totalElements: value.totalElements,
      totalPages: value.totalPages,
    }
  }
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę kont.')
}

export async function getAccounts(filters: AdminAccountFilters, signal?: AbortSignal): Promise<AdminAccountPage> {
  const params = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
  })
  if (filters.role) params.set('role', filters.role)
  if (filters.enabled !== null) params.set('enabled', String(filters.enabled))
  if (filters.query) params.set('query', filters.query)
  return adminAccountPageFrom(await apiRequest(`/api/admin/accounts?${params}`, { signal }))
}

export async function createAccount(role: CreatableStaffRole, input: ManagedAccountInput): Promise<AdminAccount> {
  const resource = role === 'ADMIN' ? 'administrators' : 'mechanics'
  return adminAccountFrom(await apiRequest(`/api/admin/accounts/${resource}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function updateAccount(accountId: number, input: AdminAccountUpdateInput): Promise<AdminAccount> {
  return adminAccountFrom(await apiRequest(`/api/admin/accounts/${accountId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function resetPassword(accountId: number, input: AdminPasswordResetInput): Promise<AdminAccount> {
  return adminAccountFrom(await apiRequest(`/api/admin/accounts/${accountId}/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function updateStatus(accountId: number, enabled: boolean): Promise<AdminAccount> {
  return adminAccountFrom(await apiRequest(`/api/admin/accounts/${accountId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled }),
  }))
}
