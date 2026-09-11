import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { errorMessage } from '../../api/apiClient'
import { AuthDialog } from '../../features/auth/components/AuthDialog'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { SiteHeader } from './SiteHeader'
import { workshopInfo } from '../../features/workshop/workshopInfo'
import { useTranslation } from '../../i18n/useTranslation'

export function AppLayout() {
  const auth = useAuth()
  const { t } = useTranslation()
  const [isLoginOpen, setIsLoginOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const canManage = auth.user?.roles.some((role) => role === 'MECHANIC' || role === 'ADMIN') ?? false
  const canAdminister = auth.user?.roles.includes('ADMIN') ?? false
  const isClient = auth.user?.roles.includes('CLIENT') ?? false

  async function handleLogout() {
    setIsLoggingOut(true)
    setLogoutError(null)
    try {
      await auth.logout()
    } catch (cause) {
      setLogoutError(errorMessage(cause))
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div id="start">
      <SiteHeader user={auth.user} isLoading={auth.isLoading} isLoggingOut={isLoggingOut}
        canManage={canManage} canAdminister={canAdminister} canViewProfile={isClient}
        onLogin={() => setIsLoginOpen(true)} onLogout={() => void handleLogout()} />
      {(auth.error || logoutError) && (
        <div className="page-content app-message">
          <p className="message error" role="alert">{auth.error || logoutError}</p>
        </div>
      )}
      <Outlet />
      <footer className="site-footer">{workshopInfo.name} · {t('app.footerOffer')}</footer>
      {isLoginOpen && !auth.user && <AuthDialog onClose={() => setIsLoginOpen(false)} />}
    </div>
  )
}
