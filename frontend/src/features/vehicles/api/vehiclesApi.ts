import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { RepairHistoryEntry, RepairItem, RepairItemType, Vehicle, VehicleInput } from '../types'

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

function isDateTime(value: unknown): value is string {
  return typeof value === 'string' && Number.isFinite(Date.parse(value))
}

function isRepairItemType(value: unknown): value is RepairItemType {
  return value === 'LABOR' || value === 'PART'
}

function isRepairItem(value: unknown): value is RepairItem {
  return isRecord(value)
    && (value.id === null || (typeof value.id === 'number' && Number.isSafeInteger(value.id)))
    && isRepairItemType(value.type)
    && typeof value.name === 'string'
    && typeof value.quantity === 'number'
    && typeof value.unitGrossAmount === 'number'
    && typeof value.totalGrossAmount === 'number'
}

function isRepairHistoryEntry(value: unknown): value is RepairHistoryEntry {
  return isRecord(value)
    && typeof value.appointmentId === 'number'
    && Number.isSafeInteger(value.appointmentId)
    && typeof value.appointmentReference === 'string'
    && isDateTime(value.visitDate)
    && typeof value.repairDescription === 'string'
    && typeof value.totalGrossAmount === 'number'
    && Array.isArray(value.repairItems)
    && value.repairItems.every(isRepairItem)
    && isDateTime(value.repairCompletedAt)
    && typeof value.repairCompletedBy === 'string'
    && isDateTime(value.vehiclePickedUpAt)
    && typeof value.vehiclePickedUpBy === 'string'
}

function repairHistoryFrom(value: unknown): RepairHistoryEntry[] {
  if (Array.isArray(value) && value.every(isRepairHistoryEntry)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową historię napraw.')
}

export async function getVehicles(signal?: AbortSignal): Promise<Vehicle[]> {
  const payload = await apiRequest('/api/vehicles', { signal })
  if (Array.isArray(payload) && payload.every(isVehicle)) return payload
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę pojazdów.')
}

export async function getVehicle(vehicleId: number, signal?: AbortSignal): Promise<Vehicle> {
  return vehicleFrom(await apiRequest(`/api/vehicles/${vehicleId}`, { signal }))
}

export async function getRepairHistory(vehicleId: number, signal?: AbortSignal): Promise<RepairHistoryEntry[]> {
  return repairHistoryFrom(await apiRequest(`/api/vehicles/${vehicleId}/repair-history`, { signal }))
}

export async function createVehicle(input: VehicleInput): Promise<Vehicle> {
  return vehicleFrom(await apiRequest('/api/vehicles', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function downloadRepairInvoice(vehicleId: number, appointmentId: number): Promise<Blob> {
  const response = await fetch(`/api/vehicles/${vehicleId}/repair-history/${appointmentId}/invoice`, {
    credentials: 'same-origin',
    cache: 'no-store',
  })
  if (!response.ok) {
    const payload: unknown = await response.json().catch(() => null)
    const message = isRecord(payload) && typeof payload.message === 'string'
      ? payload.message
      : 'Nie udało się pobrać faktury.'
    throw new ApiError(response.status, message)
  }
  return response.blob()
}

export function repairInvoiceFilename(entry: RepairHistoryEntry): string {
  return `invoice-${entry.appointmentReference}.pdf`
}
