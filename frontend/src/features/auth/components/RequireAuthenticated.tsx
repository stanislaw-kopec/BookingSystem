import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function RequireAuthenticated({ children }: { children: ReactNode }) {
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

  return auth.user ? children : <Navigate to="/" replace />
}
