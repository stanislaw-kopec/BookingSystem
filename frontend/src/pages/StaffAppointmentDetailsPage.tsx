import { Link, useParams } from 'react-router-dom'
import { StaffAppointmentDetailsSection } from '../features/appointments/components/StaffAppointmentDetailsSection'

export function StaffAppointmentDetailsPage() {
  const { appointmentId } = useParams()
  const numericAppointmentId = Number(appointmentId)

  if (!Number.isSafeInteger(numericAppointmentId) || numericAppointmentId < 1) {
    return (
      <main className="page-content">
        <section className="page-section appointments-section">
          <p className="message error" role="alert">Nieprawidłowy identyfikator zgłoszenia.</p>
          <Link className="button secondary" to="/staff/schedule">Wróć do grafiku</Link>
        </section>
      </main>
    )
  }

  return (
    <main className="page-content">
      <StaffAppointmentDetailsSection appointmentId={numericAppointmentId} />
    </main>
  )
}
