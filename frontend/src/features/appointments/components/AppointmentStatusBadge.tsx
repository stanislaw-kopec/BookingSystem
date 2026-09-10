import type { AppointmentStatus } from '../types'

const labels: Record<AppointmentStatus, string> = {
  PENDING: 'Oczekujące',
  TIME_PROPOSED: 'Zaproponowano nowy dzień',
  CONFIRMED: 'Potwierdzone',
  READY_FOR_PICKUP: 'Czeka na odbiór',
  COMPLETED: 'Zakończone',
  CANCELLED: 'Odwołane',
  REJECTED: 'Odrzucone',
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={`appointment-status status-${status.toLowerCase()}`}>{labels[status]}</span>
}
