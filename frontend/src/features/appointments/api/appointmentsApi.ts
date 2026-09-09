import { ApiError, apiRequest, isRecord } from '../../../api/apiClient'
import type {
  Appointment,
  AppointmentAvailability,
  AppointmentDay,
  AppointmentRequesterType,
  AppointmentStatus,
  ClientAppointmentInput,
  GuestAppointmentInput,
} from '../types'

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
    || value === 'CANCELLED'
    || value === 'REJECTED'
}

function isRequesterType(value: unknown): value is AppointmentRequesterType {
  return value === 'CLIENT' || value === 'GUEST'
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
}

function appointmentFrom(value: unknown): Appointment {
  if (isAppointment(value)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłowe dane zgłoszenia.')
}

function appointmentsFrom(value: unknown): Appointment[] {
  if (Array.isArray(value) && value.every(isAppointment)) return value
  throw new ApiError(502, 'Serwer zwrócił nieprawidłową listę zgłoszeń.')
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

export async function getClientAppointments(signal?: AbortSignal): Promise<Appointment[]> {
  return appointmentsFrom(await apiRequest('/api/appointments', { signal }))
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

export async function getStaffAppointments(signal?: AbortSignal): Promise<Appointment[]> {
  return appointmentsFrom(await apiRequest('/api/staff/appointments', { signal }))
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
