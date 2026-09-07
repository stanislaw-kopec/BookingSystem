import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { ClientProfile, ClientProfileInput } from '../types'

const stringFields = [
  'firstName', 'lastName', 'phoneNumber', 'contactEmail', 'addressLine', 'postalCode', 'city',
  'companyName', 'taxId', 'billingAddressLine', 'billingPostalCode', 'billingCity',
] as const

function isProfile(value: unknown): value is ClientProfile {
  return isRecord(value)
    && typeof value.configured === 'boolean'
    && typeof value.hasCompanyData === 'boolean'
    && stringFields.every((field) => typeof value[field] === 'string')
}

function profileFrom(value: unknown): ClientProfile {
  if (isProfile(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane profilu.')
}

export async function getProfile(signal?: AbortSignal): Promise<ClientProfile> {
  return profileFrom(await apiRequest('/api/profile/me', { signal }))
}

export async function saveProfile(input: ClientProfileInput): Promise<ClientProfile> {
  return profileFrom(await apiRequest('/api/profile/me', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}
