import { useState } from 'react'
import { errorMessage } from '../api/apiClient'
import { SiteHeader } from '../components/layout/SiteHeader'
import { AuthDialog } from '../features/auth/components/AuthDialog'
import { useAuth } from '../features/auth/hooks/useAuth'
import { CatalogManager } from '../features/services/components/CatalogManager'
import { ClientProfileSection } from '../features/profile/components/ClientProfileSection'
import { ServicesSection } from '../features/services/components/ServicesSection'
import { useServiceCatalog } from '../features/services/hooks/useServiceCatalog'
import { WorkshopOverview } from '../features/workshop/components/WorkshopOverview'

export function HomePage() {
  const auth = useAuth()
  const catalog = useServiceCatalog()
  const [isLoginOpen, setIsLoginOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const canManage = auth.user?.roles.some((role) => role === 'MECHANIC' || role === 'ADMIN') ?? false
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
        canManage={canManage} canViewProfile={isClient}
        onLogin={() => setIsLoginOpen(true)} onLogout={() => void handleLogout()} />
      <main className="page-content">
        {(auth.error || logoutError) && <p className="message error" role="alert">{auth.error || logoutError}</p>}
        <WorkshopOverview />
        <ServicesSection categories={catalog.categories} isLoading={catalog.isLoading}
          error={catalog.error} onRetry={catalog.reload} />
        {isClient && auth.user && <ClientProfileSection key={auth.user.username} />}
        {canManage && auth.user && !catalog.error && (
          <CatalogManager key={auth.user.username} categories={catalog.categories}
            isRefreshing={catalog.isLoading} onChanged={catalog.reload} />
        )}
      </main>
      <footer className="site-footer">Auto Serwis · Oferta warsztatu samochodowego</footer>
      {isLoginOpen && !auth.user && <AuthDialog onClose={() => setIsLoginOpen(false)} />}
    </div>
  )
}
