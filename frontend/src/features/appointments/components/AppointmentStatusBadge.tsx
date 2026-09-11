import type { AppointmentStatus } from '../types'
import { appointmentStatusTranslationKeys } from '../../../i18n/translations'
import { useTranslation } from '../../../i18n/useTranslation'

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  const { t } = useTranslation()
  return <span className={`appointment-status status-${status.toLowerCase()}`}>{t(appointmentStatusTranslationKeys[status])}</span>
}
