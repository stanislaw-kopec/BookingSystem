export type AppointmentStatus = 'PENDING' | 'TIME_PROPOSED' | 'CONFIRMED' | 'READY_FOR_PICKUP' | 'COMPLETED' | 'CANCELLED' | 'REJECTED'

export type AppointmentRequesterType = 'CLIENT' | 'GUEST'

export interface AppointmentDay {
  date: string
  startAt: string
  endAt: string
  capacity: number
  remainingCapacity: number
  available: boolean
}

export interface AppointmentAvailability {
  timeZone: string
  dailyCapacity: number
  days: AppointmentDay[]
}

export interface ClientAppointmentInput {
  vehicleId: number
  visitDate: string
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
  visitDate: string
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
  repairDescription: string
  totalGrossAmount: number | null
  repairCompletedAt: string | null
  repairCompletedBy: string | null
  vehiclePickedUpAt: string | null
  vehiclePickedUpBy: string | null
}

export interface AppointmentPage {
  content: Appointment[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CompleteRepairInput {
  repairDescription: string
  totalGrossAmount: number
}
