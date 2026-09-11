import { Link, NavLink } from 'react-router-dom'
import { useEffect, useRef } from 'react'
import type { MouseEvent } from 'react'
import type { CurrentUser } from '../../features/auth/types'
import { WorkshopLogo } from '../../features/workshop/components/WorkshopLogo'
import { workshopInfo } from '../../features/workshop/workshopInfo'
import { languages } from '../../i18n/translations'
import { useTranslation } from '../../i18n/useTranslation'

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
  const accountMenuRef = useRef<HTMLDetailsElement | null>(null)
  const { language, setLanguage, t } = useTranslation()

  useEffect(() => {
    function closeOnOutsidePointer(event: PointerEvent) {
      const menu = accountMenuRef.current
      if (!menu?.open) return
      if (event.target instanceof Node && menu.contains(event.target)) return
      menu.removeAttribute('open')
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') accountMenuRef.current?.removeAttribute('open')
    }

    document.addEventListener('pointerdown', closeOnOutsidePointer)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePointer)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  function closeAccountMenu(event: MouseEvent<HTMLAnchorElement>) {
    event.currentTarget.closest('details')?.removeAttribute('open')
  }

  const accountLabel = canViewProfile
    ? t('header.clientAccount')
    : canManage
      ? t('header.staffPanel')
      : t('header.account')

  return (
    <header className="site-header">
      <Link className="brand" to="/" aria-label={t('header.homeAria', { name: workshopInfo.name })}>
        <WorkshopLogo className="brand-logo" />
      </Link>
      <nav className="primary-nav" aria-label={t('header.mainMenuAria')}>
        <a className="primary-nav-link" href="/#about">{t('header.workshop')}</a>
        <a className="primary-nav-link" href="/#services">{t('header.services')}</a>
        <a className="primary-nav-link" href="/#location">{t('header.location')}</a>
        <NavLink className={({ isActive }) =>
          isActive ? 'primary-nav-link active' : 'primary-nav-link'} to="/appointments">
          {t('header.appointments')}
        </NavLink>
      </nav>
      <div className="language-switcher" aria-label={t('header.languageLabel')}>
        {languages.map((item) => (
          <button key={item.code} type="button"
            className={item.code === language ? 'language-switcher-button active' : 'language-switcher-button'}
            aria-pressed={item.code === language}
            onClick={() => setLanguage(item.code)}>
            {item.label}
          </button>
        ))}
      </div>
      <div className="account-menu">
        {user ? (
          <details className="account-dropdown" ref={accountMenuRef}>
            <summary className="account-trigger" aria-label={t('header.accountMenuAria', { username: user.username })}>
              <span className="account-trigger-text">
                <span className="account-trigger-label">{accountLabel}</span>
                <strong>{user.username}</strong>
              </span>
            </summary>
            <div className="account-dropdown-panel">
              <div className="account-dropdown-header">
                <span>{t('header.signedInAs')}</span>
                <strong>{user.username}</strong>
              </div>
              {canViewProfile && (
                <>
                  <p className="account-dropdown-section">{t('header.myAccount')}</p>
                  <NavLink className={accountLinkClassName} to="/profile" onClick={closeAccountMenu}>
                    {t('header.myProfile')}
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/vehicles" onClick={closeAccountMenu}>
                    {t('header.myVehicles')}
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/my-appointments" onClick={closeAccountMenu}>
                    {t('header.myAppointments')}
                  </NavLink>
                </>
              )}
              {canManage && (
                <>
                  <p className="account-dropdown-section">{t('header.workshopPanel')}</p>
                  <NavLink className={accountLinkClassName} to="/staff/schedule" onClick={closeAccountMenu}>
                    {t('header.schedule')}
                  </NavLink>
                  <NavLink className={accountLinkClassName} to="/staff/appointments" onClick={closeAccountMenu}>
                    {t('header.staffAppointments')}
                  </NavLink>
                  <a className="account-dropdown-item" href="/#service-management" onClick={closeAccountMenu}>
                    {t('header.manageServices')}
                  </a>
                  {canAdminister && (
                    <>
                      <NavLink className={accountLinkClassName} to="/admin/schedule-settings" onClick={closeAccountMenu}>
                        {t('header.scheduleSettings')}
                      </NavLink>
                      <NavLink className={accountLinkClassName} to="/admin/staff-accounts" onClick={closeAccountMenu}>
                        {t('header.staffAccounts')}
                      </NavLink>
                    </>
                  )}
                </>
              )}
              <button type="button" className="account-dropdown-item logout" disabled={isLoggingOut} onClick={() => { accountMenuRef.current?.removeAttribute('open'); onLogout() }}>
                {isLoggingOut ? t('header.loggingOut') : t('header.logout')}
              </button>
            </div>
          </details>
        ) : (
          <button type="button" className="button" disabled={isLoading} onClick={onLogin}>
            {isLoading ? t('header.checkingSession') : t('header.loginRegister')}
          </button>
        )}
      </div>
    </header>
  )
}
