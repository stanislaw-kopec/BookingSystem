import type { AppointmentStatus, RepairItemType } from './types'

const appointmentStatusLabels: Record<AppointmentStatus, string> = {
  PENDING: 'Oczekujące',
  TIME_PROPOSED: 'Zaproponowano nowy dzień',
  CONFIRMED: 'Potwierdzone',
  READY_FOR_PICKUP: 'Czeka na odbiór',
  COMPLETED: 'Zakończone',
  CANCELLED: 'Odwołane',
  REJECTED: 'Odrzucone',
}

const repairItemTypeLabels: Record<RepairItemType, string> = {
  LABOR: 'Robocizna',
  PART: 'Część',
}

export function appointmentStatusLabel(status: AppointmentStatus) {
  return appointmentStatusLabels[status]
}

export function repairItemTypeLabel(type: RepairItemType) {
  return repairItemTypeLabels[type]
}
