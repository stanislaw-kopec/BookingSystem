import { useAuth } from '../features/auth/hooks/useAuth'
import { ClientProfileSection } from '../features/profile/components/ClientProfileSection'

export function ProfilePage() {
  const auth = useAuth()

  return (
    <main className="page-content">
      <ClientProfileSection key={auth.user?.username} />
    </main>
  )
}
