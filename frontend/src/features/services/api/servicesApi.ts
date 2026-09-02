import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { CategoryInput, ServiceCategory, ServiceInput, WorkshopService } from '../types'

function isService(value: unknown): value is WorkshopService {
  return isRecord(value) && Number.isSafeInteger(value.id) && Number.isSafeInteger(value.categoryId)
    && typeof value.name === 'string' && typeof value.description === 'string'
}

function isCategory(value: unknown): value is ServiceCategory {
  return isRecord(value) && Number.isSafeInteger(value.id)
    && typeof value.name === 'string' && typeof value.description === 'string'
    && Array.isArray(value.services) && value.services.every(isService)
}

export async function getCatalog(signal: AbortSignal): Promise<ServiceCategory[]> {
  const payload = await apiRequest('/api/services', { signal })
  if (!Array.isArray(payload) || !payload.every(isCategory)) {
    throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę usług.')
  }
  return payload
}

export async function saveCategory(input: CategoryInput, id?: number): Promise<void> {
  await apiRequest(id === undefined ? '/api/service-categories' : '/api/service-categories/' + id, {
    method: id === undefined ? 'POST' : 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
}

export async function saveService(input: ServiceInput, id?: number): Promise<void> {
  await apiRequest(id === undefined ? '/api/services' : '/api/services/' + id, {
    method: id === undefined ? 'POST' : 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
}

export async function deleteCategory(id: number): Promise<void> {
  await apiRequest('/api/service-categories/' + id, { method: 'DELETE' })
}

export async function deleteService(id: number): Promise<void> {
  await apiRequest('/api/services/' + id, { method: 'DELETE' })
}
