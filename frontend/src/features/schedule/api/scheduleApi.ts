import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type { ScheduleDayOverride, ScheduleDayOverrideInput, ScheduleSettings, WorkshopScheduleConfig } from '../types'

function isTime(value: unknown): value is string {
  return typeof value === 'string' && /^\d{2}:\d{2}(:\d{2})?$/.test(value)
}

function isSettings(value: unknown): value is ScheduleSettings {
  return isRecord(value)
    && typeof value.defaultDailyCapacity === 'number'
    && Number.isSafeInteger(value.defaultDailyCapacity)
    && typeof value.bookingHorizonDays === 'number'
    && Number.isSafeInteger(value.bookingHorizonDays)
    && isTime(value.workdayStart)
    && isTime(value.workdayEnd)
}

function isOverride(value: unknown): value is ScheduleDayOverride {
  return isRecord(value)
    && typeof value.id === 'number'
    && Number.isSafeInteger(value.id)
    && typeof value.date === 'string'
    && typeof value.capacity === 'number'
    && Number.isSafeInteger(value.capacity)
    && typeof value.closed === 'boolean'
    && typeof value.note === 'string'
}

function isConfig(value: unknown): value is WorkshopScheduleConfig {
  return isRecord(value)
    && typeof value.timeZone === 'string'
    && isSettings(value.settings)
    && Array.isArray(value.overrides)
    && value.overrides.every(isOverride)
}

function configFrom(value: unknown): WorkshopScheduleConfig {
  if (isConfig(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową konfigurację grafiku.')
}

export async function getScheduleConfig(signal?: AbortSignal): Promise<WorkshopScheduleConfig> {
  return configFrom(await apiRequest('/api/admin/schedule', { signal }))
}

export async function updateScheduleSettings(settings: ScheduleSettings): Promise<WorkshopScheduleConfig> {
  return configFrom(await apiRequest('/api/admin/schedule/settings', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  }))
}

export async function saveScheduleOverride(input: ScheduleDayOverrideInput): Promise<WorkshopScheduleConfig> {
  return configFrom(await apiRequest(`/api/admin/schedule/overrides/${input.date}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function deleteScheduleOverride(date: string): Promise<WorkshopScheduleConfig> {
  return configFrom(await apiRequest(`/api/admin/schedule/overrides/${date}`, { method: 'DELETE' }))
}
