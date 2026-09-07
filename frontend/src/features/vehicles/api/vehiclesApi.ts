import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { Vehicle, VehicleInput } from '../types'

function isVehicle(value: unknown): value is Vehicle {
  return isRecord(value)
    && typeof value.id === 'number'
    && Number.isSafeInteger(value.id)
    && typeof value.make === 'string'
    && typeof value.model === 'string'
    && typeof value.productionYear === 'number'
    && Number.isInteger(value.productionYear)
    && typeof value.registrationNumber === 'string'
    && typeof value.vin === 'string'
}

function vehicleFrom(value: unknown): Vehicle {
  if (isVehicle(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane pojazdu.')
}

export async function getVehicles(signal?: AbortSignal): Promise<Vehicle[]> {
  const payload = await apiRequest('/api/vehicles', { signal })
  if (Array.isArray(payload) && payload.every(isVehicle)) return payload
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę pojazdów.')
}

export async function getVehicle(vehicleId: number, signal?: AbortSignal): Promise<Vehicle> {
  return vehicleFrom(await apiRequest(`/api/vehicles/${vehicleId}`, { signal }))
}

export async function createVehicle(input: VehicleInput): Promise<Vehicle> {
  return vehicleFrom(await apiRequest('/api/vehicles', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}
