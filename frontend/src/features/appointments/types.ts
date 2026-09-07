export type AppointmentStatus = 'PENDING' | 'TIME_PROPOSED' | 'CONFIRMED' | 'REJECTED'

export type AppointmentRequesterType = 'CLIENT' | 'GUEST'

export interface AppointmentSlot {
  startAt: string
  endAt: string
  available: boolean
}

export interface AppointmentAvailability {
  timeZone: string
  slotDurationMinutes: number
  slots: AppointmentSlot[]
}

export interface ClientAppointmentInput {
  vehicleId: number
  slotStartAt: string
  problemDescription: string
}

export interface GuestAppointmentInput {
  firstName: string
  lastName: string
  phoneNumber: string
  contactEmail: string
  vehicleMake: string
  vehicleModel: string
  vehicleProductionYear: number
  vehicleRegistrationNumber: string
  vehicleVin: string
  slotStartAt: string
  problemDescription: string
}

export interface Appointment {
  id: number
  reference: string
  requesterType: AppointmentRequesterType
  status: AppointmentStatus
  vehicleId: number | null
  vehicleMake: string
  vehicleModel: string
  vehicleProductionYear: number
  vehicleRegistrationNumber: string
  vehicleVin: string
  firstName: string
  lastName: string
  phoneNumber: string
  contactEmail: string
  requestedStartAt: string
  currentStartAt: string
  problemDescription: string
  staffMessage: string
  createdAt: string
  staffActionAt: string | null
  staffActionBy: string | null
  clientConfirmedAt: string | null
}
