import type { AppointmentStatus } from '../types'

const labels: Record<AppointmentStatus, string> = {
  PENDING: 'Oczekujące',
  TIME_PROPOSED: 'Zaproponowano nowy termin',
  CONFIRMED: 'Potwierdzone',
  REJECTED: 'Odrzucone',
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={`appointment-status status-${status.toLowerCase()}`}>{labels[status]}</span>
}
