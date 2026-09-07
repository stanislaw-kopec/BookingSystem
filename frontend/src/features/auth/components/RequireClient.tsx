import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function RequireClient({ children }: { children: ReactNode }) {
  const auth = useAuth()

  if (auth.isLoading) {
    return (
      <main className="page-content">
        <section className="page-section">
          <p role="status">Sprawdzanie sesji…</p>
        </section>
      </main>
    )
  }

  const isClient = auth.user?.roles.includes('CLIENT') ?? false
  return isClient ? children : <Navigate to="/" replace />
}
