import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type {
  Appointment,
  AppointmentAvailability,
  AppointmentDay,
  AppointmentPage,
  AppointmentRequesterType,
  AppointmentStatus,
  RepairItem,
  RepairItemInput,
  RepairItemType,
  ClientAppointmentInput,
  GuestAppointmentInput,
} from '../types'
import type { RepairHistoryEntry } from '../../vehicles/types'

function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isDateTime(value: unknown): value is string {
  return typeof value === 'string' && Number.isFinite(Date.parse(value))
}

function isAppointmentStatus(value: unknown): value is AppointmentStatus {
  return value === 'PENDING'
    || value === 'TIME_PROPOSED'
    || value === 'CONFIRMED'
    || value === 'READY_FOR_PICKUP'
    || value === 'COMPLETED'
    || value === 'CANCELLED'
    || value === 'REJECTED'
}

function isRequesterType(value: unknown): value is AppointmentRequesterType {
  return value === 'CLIENT' || value === 'GUEST'
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

function isDay(value: unknown): value is AppointmentDay {
  return isRecord(value)
    && typeof value.date === 'string'
    && isDateTime(value.startAt)
    && isDateTime(value.endAt)
    && typeof value.capacity === 'number'
    && Number.isSafeInteger(value.capacity)
    && typeof value.remainingCapacity === 'number'
    && Number.isSafeInteger(value.remainingCapacity)
    && typeof value.available === 'boolean'
}

function isAvailability(value: unknown): value is AppointmentAvailability {
  return isRecord(value)
    && typeof value.timeZone === 'string'
    && typeof value.dailyCapacity === 'number'
    && Number.isSafeInteger(value.dailyCapacity)
    && Array.isArray(value.days)
    && value.days.every(isDay)
}

function availabilityFrom(value: unknown): AppointmentAvailability {
  if (isAvailability(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę dni wizyt.')
}

function isAppointment(value: unknown): value is Appointment {
  return isRecord(value)
    && typeof value.id === 'number'
    && Number.isSafeInteger(value.id)
    && typeof value.reference === 'string'
    && isRequesterType(value.requesterType)
    && isAppointmentStatus(value.status)
    && (value.vehicleId === null || (typeof value.vehicleId === 'number' && Number.isSafeInteger(value.vehicleId)))
    && typeof value.vehicleMake === 'string'
    && typeof value.vehicleModel === 'string'
    && typeof value.vehicleProductionYear === 'number'
    && Number.isInteger(value.vehicleProductionYear)
    && typeof value.vehicleRegistrationNumber === 'string'
    && typeof value.vehicleVin === 'string'
    && typeof value.firstName === 'string'
    && typeof value.lastName === 'string'
    && typeof value.phoneNumber === 'string'
    && typeof value.contactEmail === 'string'
    && isDateTime(value.requestedStartAt)
    && isDateTime(value.currentStartAt)
    && typeof value.problemDescription === 'string'
    && typeof value.staffMessage === 'string'
    && isDateTime(value.createdAt)
    && (value.staffActionAt === null || isDateTime(value.staffActionAt))
    && isNullableString(value.staffActionBy)
    && (value.clientConfirmedAt === null || isDateTime(value.clientConfirmedAt))
    && typeof value.repairDescription === 'string'
    && (value.totalGrossAmount === null || typeof value.totalGrossAmount === 'number')
    && Array.isArray(value.repairItems)
    && value.repairItems.every(isRepairItem)
    && (value.repairCompletedAt === null || isDateTime(value.repairCompletedAt))
    && isNullableString(value.repairCompletedBy)
    && (value.vehiclePickedUpAt === null || isDateTime(value.vehiclePickedUpAt))
    && isNullableString(value.vehiclePickedUpBy)
}

function appointmentFrom(value: unknown): Appointment {
  if (isAppointment(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane zgłoszenia.')
}

function appointmentsFrom(value: unknown): Appointment[] {
  if (Array.isArray(value) && value.every(isAppointment)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę zgłoszeń.')
}

function isAppointmentPage(value: unknown): value is AppointmentPage {
  return isRecord(value)
    && Array.isArray(value.content)
    && value.content.every(isAppointment)
    && typeof value.page === 'number'
    && Number.isSafeInteger(value.page)
    && typeof value.size === 'number'
    && Number.isSafeInteger(value.size)
    && typeof value.totalElements === 'number'
    && Number.isSafeInteger(value.totalElements)
    && typeof value.totalPages === 'number'
    && Number.isSafeInteger(value.totalPages)
}

function appointmentPageFrom(value: unknown): AppointmentPage {
  if (isAppointmentPage(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową stronę zgłoszeń.')
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

export async function getAvailability(signal?: AbortSignal): Promise<AppointmentAvailability> {
  return availabilityFrom(await apiRequest('/api/appointments/availability', { signal }))
}

export async function createClientAppointment(input: ClientAppointmentInput): Promise<Appointment> {
  return appointmentFrom(await apiRequest('/api/appointments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function createGuestAppointment(input: GuestAppointmentInput): Promise<Appointment> {
  return appointmentFrom(await apiRequest('/api/appointments/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function getClientAppointments(
  page: number,
  size: number,
  sortDirection: 'ASC' | 'DESC',
  status: AppointmentStatus | 'ALL',
  signal?: AbortSignal,
): Promise<AppointmentPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortDirection,
  })
  if (status !== 'ALL') params.set('status', status)
  return appointmentPageFrom(await apiRequest(`/api/appointments?${params}`, { signal }))
}

export async function confirmProposedTime(appointmentId: number): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/appointments/${appointmentId}/confirm-proposed`, {
    method: 'POST',
  }))
}

export async function cancelClientAppointment(appointmentId: number): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/appointments/${appointmentId}/cancel`, {
    method: 'POST',
  }))
}

export async function getStaffAppointments(
  page: number,
  size: number,
  sortDirection: 'ASC' | 'DESC',
  status: AppointmentStatus | 'ALL',
  signal?: AbortSignal,
): Promise<AppointmentPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortDirection,
  })
  if (status !== 'ALL') params.set('status', status)
  return appointmentPageFrom(await apiRequest(`/api/staff/appointments?${params}`, { signal }))
}

export async function getAllStaffAppointments(signal?: AbortSignal): Promise<Appointment[]> {
  return appointmentsFrom(await apiRequest('/api/staff/appointments/all', { signal }))
}

export async function getStaffAppointment(appointmentId: number, signal?: AbortSignal): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}`, { signal }))
}

export async function getStaffAppointmentRepairHistory(
  appointmentId: number,
  signal?: AbortSignal,
): Promise<RepairHistoryEntry[]> {
  return repairHistoryFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/repair-history`, { signal }))
}

export async function acceptAppointment(appointmentId: number): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/accept`, {
    method: 'POST',
  }))
}

export async function rejectAppointment(appointmentId: number, message: string): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  }))
}

export async function proposeAppointmentTime(
  appointmentId: number,
  visitDate: string,
  message: string,
): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/propose-time`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ visitDate, message }),
  }))
}

export async function confirmGuestProposedTime(appointmentId: number): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/confirm-proposed`, {
    method: 'POST',
  }))
}

export async function completeRepair(
  appointmentId: number,
  repairDescription: string,
  repairItems: RepairItemInput[],
): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/complete-repair`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repairDescription, repairItems }),
  }))
}

export async function markVehiclePickedUp(appointmentId: number): Promise<Appointment> {
  return appointmentFrom(await apiRequest(`/api/staff/appointments/${appointmentId}/mark-picked-up`, {
    method: 'POST',
  }))
}


export async function downloadStaffRepairInvoice(appointmentId: number): Promise<Blob> {
  const response = await fetch(`/api/staff/appointments/${appointmentId}/invoice`, {
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

export function staffRepairInvoiceFilename(appointment: { reference: string }): string {
  return `invoice-${appointment.reference}.pdf`
}
