import { ClientAppointmentsSection } from '../features/appointments/components/ClientAppointmentsSection'
import { useAuth } from '../features/auth/hooks/useAuth'

export function MyAppointmentsPage() {
  const { user } = useAuth()

  return (
    <main className="page-content">
      <ClientAppointmentsSection key={user?.username} />
    </main>
  )
}
