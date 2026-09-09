import { useAuth } from '../features/auth/hooks/useAuth'
import { CatalogManager } from '../features/services/components/CatalogManager'
import { ServicesSection } from '../features/services/components/ServicesSection'
import { useServiceCatalog } from '../features/services/hooks/useServiceCatalog'
import { WorkshopOverview } from '../features/workshop/components/WorkshopOverview'
import { WorkshopLocation } from '../features/workshop/components/WorkshopLocation'

export function HomePage() {
  const auth = useAuth()
  const catalog = useServiceCatalog()
  const canManage = auth.user?.roles.some((role) => role === 'MECHANIC' || role === 'ADMIN') ?? false

  return (
    <main className="page-content">
      <WorkshopOverview />
      <ServicesSection categories={catalog.categories} isLoading={catalog.isLoading}
        error={catalog.error} onRetry={catalog.reload} />
      {canManage && auth.user && !catalog.error && (
        <CatalogManager key={auth.user.username} categories={catalog.categories}
          isRefreshing={catalog.isLoading} onChanged={catalog.reload} />
      )}
      <WorkshopLocation />
    </main>
  )
}
