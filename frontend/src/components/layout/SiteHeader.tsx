import { Link } from 'react-router-dom'
import type { MouseEvent } from 'react'
import type { CurrentUser } from '../../features/auth/types'

interface Props {
  user: CurrentUser | null
  isLoading: boolean
  isLoggingOut: boolean
  canManage: boolean
  canViewProfile: boolean
  onLogin: () => void
  onLogout: () => void
}

export function SiteHeader({ user, isLoading, isLoggingOut, canManage, canViewProfile, onLogin, onLogout }: Props) {
  function closeAccountMenu(event: MouseEvent<HTMLAnchorElement>) {
    event.currentTarget.closest('details')?.removeAttribute('open')
  }

  return (
    <header className="site-header">
      <Link className="brand" to="/">Auto Serwis</Link>
      <nav aria-label="Menu główne">
        <a href="/#about">O warsztacie</a>
        <a href="/#services">Usługi</a>
        <a href="/#location">Lokalizacja</a>
        {canManage && <a href="/#service-management">Zarządzaj ofertą</a>}
      </nav>
      <div className="account-menu">
        {user ? (
          <details className="account-dropdown">
            <summary className="account-trigger">
              <span className="muted">Konto</span>
              <strong>{user.username}</strong>
            </summary>
            <div className="account-dropdown-panel">
              {canViewProfile && (
                <>
                  <Link className="account-dropdown-item" to="/profile" onClick={closeAccountMenu}>
                    Mój profil
                  </Link>
                  <Link className="account-dropdown-item" to="/vehicles" onClick={closeAccountMenu}>
                    Moje pojazdy
                  </Link>
                </>
              )}
              <button type="button" className="account-dropdown-item" disabled={isLoggingOut} onClick={onLogout}>
                {isLoggingOut ? 'Wylogowywanie…' : 'Wyloguj'}
              </button>
            </div>
          </details>
        ) : (
          <button type="button" className="button" disabled={isLoading} onClick={onLogin}>
            {isLoading ? 'Sprawdzanie sesji…' : 'Logowanie / Rejestracja'}
          </button>
        )}
      </div>
    </header>
  )
}
