export type AppointmentStatus = 'PENDING' | 'TIME_PROPOSED' | 'CONFIRMED' | 'READY_FOR_PICKUP' | 'COMPLETED' | 'CANCELLED' | 'REJECTED'

export type AppointmentRequesterType = 'CLIENT' | 'GUEST'

export type RepairItemType = 'LABOR' | 'PART'

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

export interface RepairItem {
  id: number | null
  type: RepairItemType
  name: string
  quantity: number
  unitGrossAmount: number
  totalGrossAmount: number
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
  repairItems: RepairItem[]
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

export interface ScheduleAppointment {
  id: number
  reference: string
  status: 'PENDING' | 'TIME_PROPOSED' | 'CONFIRMED'
  currentStartAt: string
  vehicleMake: string
  vehicleModel: string
  vehicleRegistrationNumber: string
  firstName: string
  lastName: string
  problemSummary: string
}

export interface StaffScheduleDay {
  date: string
  capacity: number
  remainingCapacity: number
  closed: boolean
  appointments: ScheduleAppointment[]
}

export interface StaffSchedule {
  timeZone: string
  days: StaffScheduleDay[]
}

export interface RepairItemInput {
  type: RepairItemType
  name: string
  quantity: number
  unitGrossAmount: number
}

export interface CompleteRepairInput {
  repairDescription: string
  repairItems: RepairItemInput[]
}
