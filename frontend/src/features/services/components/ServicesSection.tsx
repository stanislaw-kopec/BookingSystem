import type { ServiceCategory } from '../types'
import { ServiceCategoryCard } from './ServiceCategoryCard'
import '../services.css'

interface Props {
  categories: ServiceCategory[]
  isLoading: boolean
  error: string | null
  onRetry: () => void
}

export function ServicesSection({ categories, isLoading, error, onRetry }: Props) {
  return (
    <section id="services" className="page-section" aria-labelledby="services-heading">
      <div className="section-heading">
        <p className="eyebrow">Oferta warsztatu</p>
        <h2 id="services-heading">Nasze usługi</h2>
        <p className="muted">Sprawdź, w czym możemy Ci pomóc.</p>
      </div>
      {isLoading && <p role="status">Ładowanie usług…</p>}
      {error && (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button type="button" className="button secondary" onClick={onRetry}>Spróbuj ponownie</button>
        </div>
      )}
      {!isLoading && !error && categories.length === 0 && (
        <p className="empty-state">Oferta warsztatu zostanie wkrótce uzupełniona.</p>
      )}
      {!error && (
        <div className="category-grid" aria-busy={isLoading}>
          {categories.map((category) => <ServiceCategoryCard key={category.id} category={category} />)}
        </div>
      )}
    </section>
  )
}
