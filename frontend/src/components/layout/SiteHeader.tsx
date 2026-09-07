import { Link } from 'react-router-dom'
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
  return (
    <header className="site-header">
      <Link className="brand" to="/">Auto Serwis</Link>
      <nav aria-label="Menu główne">
        <a href="/#o-warsztacie">O warsztacie</a>
        <a href="/#uslugi">Usługi</a>
        <a href="/#lokalizacja">Lokalizacja</a>
        {canViewProfile && <Link to="/profil">Mój profil</Link>}
        {canManage && <a href="/#zarzadzanie-oferta">Zarządzaj ofertą</a>}
      </nav>
      <div className="account-menu">
        {user ? (
          <>
            <span className="muted">Zalogowano: <strong>{user.username}</strong></span>
            <button type="button" className="button secondary" disabled={isLoggingOut} onClick={onLogout}>
              {isLoggingOut ? 'Wylogowywanie…' : 'Wyloguj'}
            </button>
          </>
        ) : (
          <button type="button" className="button" disabled={isLoading} onClick={onLogin}>
            {isLoading ? 'Sprawdzanie sesji…' : 'Logowanie / Rejestracja'}
          </button>
        )}
      </div>
    </header>
  )
}
