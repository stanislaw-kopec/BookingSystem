import { Link, NavLink } from 'react-router-dom'
import type { MouseEvent } from 'react'
import type { CurrentUser } from '../../features/auth/types'
import { WorkshopLogo } from '../../features/workshop/components/WorkshopLogo'
import { workshopInfo } from '../../features/workshop/workshopInfo'

interface Props {
  user: CurrentUser | null
  isLoading: boolean
  isLoggingOut: boolean
  canManage: boolean
  canAdminister: boolean
  canViewProfile: boolean
  onLogin: () => void
  onLogout: () => void
}

function accountLinkClassName({ isActive }: { isActive: boolean }) {
  return isActive ? 'account-dropdown-item active' : 'account-dropdown-item'
}

export function SiteHeader({ user, isLoading, isLoggingOut, canManage, canAdminister, canViewProfile, onLogin, onLogout }: Props) {
  function closeAccountMenu(event: MouseEvent<HTMLAnchorElement>) {
    event.currentTarget.closest('details')?.removeAttribute('open')
  }

  const accountLabel = canViewProfile ? 'Konto klienta' : canManage ? 'Panel personelu' : 'Konto'

  return (
    <header className="site-header">
      <Link className="brand" to="/" aria-label={`${workshopInfo.name} — strona główna`}>
        <WorkshopLogo className="brand-logo" />
      </Link>
      <nav className="primary-nav" aria-label="Menu główne">
        <a className="primary-nav-link" href="/#about">O warsztacie</a>
        <a className="primary-nav-link" href="/#services">Usługi</a>
        <a className="primary-nav-link" href="/#location">Lokalizacja</a>
        <NavLink className={({ isActive }) =>
          isActive ? 'primary-nav-link active' : 'primary-nav-link'} to="/appointments">
          Umów wizytę
        </NavLink>
      </nav>
      <div className="account-menu">
        {user ? (
          <details className="account-dropdown">
            <summary className="account-trigger" aria-label={`Menu konta użytkownika ${user.username}`}>
              <span className="account-trigger-text">
                <span className="account-trigger-label">{accountLabel}</span>
                <strong>{user.username}</strong>
              </span>
            </summary>
            <div className="account-dropdown-panel">
              <div className="account-dropdown-header">
                <span>Zalogowano jako</span>
                <strong>{user.username}</strong>
              </div>
              {canViewProfile && (
                <>
                  <p className="account-dropdown-section">Moje konto</p>
                  <NavLink className={accountLinkClassName} to="/profile" onClick={closeAccountMenu}>
                    Mój profil
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/vehicles" onClick={closeAccountMenu}>
                    Moje pojazdy
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/my-appointments" onClick={closeAccountMenu}>
                    Moje wizyty
                  </NavLink>
                </>
              )}
              {canManage && (
                <>
                  <p className="account-dropdown-section">Panel warsztatu</p>
                  <NavLink className={accountLinkClassName} to="/staff/schedule" onClick={closeAccountMenu}>
                    Grafik
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/staff/appointments" onClick={closeAccountMenu}>
                    Zgłoszenia wizyt
                  </NavLink>
                  <a className="account-dropdown-item" href="/#service-management" onClick={closeAccountMenu}>
                    Zarządzaj ofertą
                  </a>
                  {canAdminister && (
                    <>
                      <NavLink className={accountLinkClassName} to="/admin/schedule-settings" onClick={closeAccountMenu}>
                        Konfiguracja grafiku
                      </NavLink>
                      <NavLink className={accountLinkClassName} to="/admin/staff-accounts" onClick={closeAccountMenu}>
                        Konta mechaników
                      </NavLink>
                    </>
                  )}
                </>
              )}
              <button type="button" className="account-dropdown-item logout" disabled={isLoggingOut} onClick={onLogout}>
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
