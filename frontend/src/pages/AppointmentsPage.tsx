import { Link } from 'react-router-dom'
import { ClientAppointmentsSection } from '../features/appointments/components/ClientAppointmentsSection'
import { GuestAppointmentSection } from '../features/appointments/components/GuestAppointmentSection'
import { useAuth } from '../features/auth/hooks/useAuth'

export function AppointmentsPage() {
  const auth = useAuth()

  if (auth.isLoading) {
    return (
      <main className="page-content">
        <section className="page-section"><p role="status">Sprawdzanie sesji…</p></section>
      </main>
    )
  }

  if (auth.user?.roles.includes('CLIENT')) {
    return (
      <main className="page-content">
        <ClientAppointmentsSection key={auth.user.username} />
      </main>
    )
  }

  if (auth.user) {
    return (
      <main className="page-content">
        <section className="page-section appointments-section">
          <p className="eyebrow">Konto personelu</p>
          <h2>Obsługa zgłoszeń wizyt</h2>
          <p className="muted">Zalogowane konto personelu obsługuje wizyty w swoim panelu.</p>
          <Link className="button" to="/staff/appointments">Otwórz zgłoszenia wizyt</Link>
        </section>
      </main>
    )
  }

  return (
    <main className="page-content">
      <GuestAppointmentSection />
    </main>
  )
}
