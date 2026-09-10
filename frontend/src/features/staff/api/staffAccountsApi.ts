import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { StaffAccount, StaffAccountInput, StaffAccountUpdateInput, StaffPasswordResetInput } from '../types'

function isStaffAccount(value: unknown): value is StaffAccount {
  return isRecord(value)
    && typeof value.id === 'number'
    && Number.isSafeInteger(value.id)
    && typeof value.username === 'string'
    && typeof value.email === 'string'
    && value.role === 'MECHANIC'
    && typeof value.enabled === 'boolean'
}

function staffAccountFrom(value: unknown): StaffAccount {
  if (isStaffAccount(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane konta mechanika.')
}

function staffAccountsFrom(value: unknown): StaffAccount[] {
  if (Array.isArray(value) && value.every(isStaffAccount)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę kont mechaników.')
}

export async function getMechanicAccounts(signal?: AbortSignal): Promise<StaffAccount[]> {
  return staffAccountsFrom(await apiRequest('/api/admin/staff/mechanics', { signal }))
}

export async function createMechanicAccount(input: StaffAccountInput): Promise<StaffAccount> {
  return staffAccountFrom(await apiRequest('/api/admin/staff/mechanics', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function updateMechanicAccount(mechanicId: number, input: StaffAccountUpdateInput): Promise<StaffAccount> {
  return staffAccountFrom(await apiRequest(`/api/admin/staff/mechanics/${mechanicId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function resetMechanicPassword(mechanicId: number, input: StaffPasswordResetInput): Promise<StaffAccount> {
  return staffAccountFrom(await apiRequest(`/api/admin/staff/mechanics/${mechanicId}/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function updateMechanicStatus(mechanicId: number, enabled: boolean): Promise<StaffAccount> {
  return staffAccountFrom(await apiRequest(`/api/admin/staff/mechanics/${mechanicId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled }),
  }))
}
