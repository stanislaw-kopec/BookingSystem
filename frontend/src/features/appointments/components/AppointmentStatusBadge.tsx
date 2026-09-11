import type { AppointmentStatus } from '../types'
import { appointmentStatusLabel } from '../appointmentLabels'

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={`appointment-status status-${status.toLowerCase()}`}>{appointmentStatusLabel(status)}</span>
}
